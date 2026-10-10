#!/usr/bin/env bash
# Measure the cost of the current system wallpaper on a connected device.
#
# Usage: tools/measure_wallpaper.sh <output-dir> <duration-seconds> [target-fps]
#
# The script only observes: it does not apply wallpapers, change settings,
# unplug the battery or clear logs. Put the device in the state to measure
# (wallpaper applied, home visible, app in front, screen off ...) first.
#
# Environment:
#   ADB     adb binary (default: adb on PATH)
#   SERIAL  device serial (default: ANDROID_SERIAL or the only device)
#   PKG     wallpaper app package for per-process numbers
#           (default: com.jamsmendez.cieldroid)
#   POLL    frame polling interval in seconds (default: 2). SurfaceFlinger keeps
#           128 frames and gfxinfo 120 frames, so POLL must stay below ~3 s at 30 fps.
#
# Frame sources (both recorded, see summary.txt):
#   sf   SurfaceFlinger --latency on the wallpaper surface ("Wallpaper BBQ wrapper"
#        child of the wallpaper service layer): real presents of wallpaper buffers.
#   hwui gfxinfo framestats of $PKG: HWUI frames of the Presentation window that
#        hosts the WebView (in-process WebView GPU work included).
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <output-dir> <duration-seconds> [target-fps]" >&2
  exit 2
fi
OUT=$1
DURATION=$2
TARGET_FPS=${3:-30}
ADB=${ADB:-adb}
PKG=${PKG:-com.jamsmendez.cieldroid}
POLL=${POLL:-2}
SERIAL=${SERIAL:-${ANDROID_SERIAL:-}}
adb_() { if [[ -n $SERIAL ]]; then "$ADB" -s "$SERIAL" "$@"; else "$ADB" "$@"; fi; }
sh_() { adb_ shell "$@" | tr -d '\r'; }
ts() { date -u +%Y-%m-%dT%H:%M:%S.%3NZ; }

mkdir -p "$OUT/frames"
log() { echo "$(ts) $*" | tee -a "$OUT/timeline.txt"; }
: > "$OUT/timeline.txt"
log "start out=$OUT duration=${DURATION}s target_fps=$TARGET_FPS pkg=$PKG"

# --- identity and state -----------------------------------------------------
{
  echo "host_time=$(ts)"
  echo "serial=$(adb_ get-serialno)"
  echo "model=$(sh_ getprop ro.product.model)"
  echo "android=$(sh_ getprop ro.build.version.release) api=$(sh_ getprop ro.build.version.sdk)"
  echo "build=$(sh_ getprop ro.build.fingerprint)"
  sh_ dumpsys webviewupdate | grep -i "Current WebView package" || true
  sh_ dumpsys SurfaceFlinger | grep -m1 "^GLES" || true
  echo "app_version=$(sh_ dumpsys package "$PKG" | grep -m1 versionName | xargs || true)"
} > "$OUT/identity.txt"

state() {
  {
    echo "## $1 $(ts)"
    sh_ dumpsys wallpaper | grep -E "wallpaper state:|mWhich|mWallpaperComponent=" || true
    sh_ dumpsys power | grep -E "mWakefulness=|mStayOn=" || true
    sh_ dumpsys window | grep -m1 mCurrentFocus || true
    echo "rotation=$(sh_ settings get system user_rotation) auto=$(sh_ settings get system accelerometer_rotation)"
  } >> "$OUT/state.txt"
}
: > "$OUT/state.txt"
state start

# --- processes ----------------------------------------------------------------
APP_PID=$(sh_ pidof "$PKG" || true)
RENDERER_PIDS=$(sh_ dumpsys activity services "$PKG" \
  | grep -oE 'app=ProcessRecord\{[0-9a-f]+ [0-9]+:com\.google\.android\.webview:sandboxed' \
  | grep -oE ' [0-9]+:' | tr -d ' :' | sort -u | xargs || true)
SF_PID=$(sh_ pidof surfaceflinger || true)
SYSUI_PID=$(sh_ pidof com.android.systemui || true)
{
  echo "app=${APP_PID:-none}"
  echo "renderer=${RENDERER_PIDS:-none}"
  echo "surfaceflinger=${SF_PID:-none}"
  echo "systemui=${SYSUI_PID:-none}"
  echo "note=Android WebView runs its GPU thread inside the app process; there is no separate GPU process."
} > "$OUT/pids.txt"
ALL_PIDS="$APP_PID $RENDERER_PIDS $SF_PID $SYSUI_PID"

cpu_snapshot() {  # $1 = file
  sh_ "head -1 /proc/stat; for p in $ALL_PIDS; do echo \"pid \$p \$(cut -d' ' -f14,15 /proc/\$p/stat 2>/dev/null)\"; done" > "$1"
}
mem_snapshot() {  # $1 = label
  for p in $APP_PID $RENDERER_PIDS; do
    [[ -n $p ]] || continue
    echo "pid $p $1 $(sh_ dumpsys meminfo "$p" | grep -m1 -E 'TOTAL PSS:|^ *TOTAL ' | xargs)"
  done >> "$OUT/meminfo.txt"
}
thermal() {  # $1 = label
  { echo "## $1 $(ts)"; sh_ dumpsys thermalservice \
      | sed -n '/^Thermal Status/p;/Current temperatures from HAL/,/Current cooling/p'; } >> "$OUT/thermal.txt"
}

# --- wallpaper surface layer --------------------------------------------------
WP_CLASS=$(sh_ dumpsys wallpaper | grep -m1 -oE 'mWallpaperComponent=ComponentInfo\{[^}]+' | sed 's/.*\///')
sh_ dumpsys SurfaceFlinger --list > "$OUT/sf-layers.txt"
LAYER=$(python3 -I - "$OUT/sf-layers.txt" "$WP_CLASS" <<'PY' || true
import re, sys
rows = [re.match(r'RequestedLayerState\{(.*)#(\d+)(?: parentId=(\d+))?', l) for l in open(sys.argv[1])]
rows = [(m.group(1), m.group(2), m.group(3)) for m in rows if m]
svc = {i for n, i, p in rows if n.endswith(sys.argv[2])}
hits = [f"{n}#{i}" for n, i, p in rows if n == 'Wallpaper BBQ wrapper' and p in svc]
print(hits[-1] if hits else '')
PY
)
echo "wallpaper_class=$WP_CLASS layer=${LAYER:-none}" >> "$OUT/pids.txt"

# --- measurement window -------------------------------------------------------
LOG_SINCE=$(sh_ "date +'%m-%d %H:%M:%S.000'")
thermal start
mem_snapshot start
cpu_snapshot "$OUT/cpu-start.txt"
[[ -n $APP_PID ]] && sh_ dumpsys gfxinfo "$PKG" reset > /dev/null
# Frames already in the SurfaceFlinger ring before the window are excluded.
[[ -n $LAYER ]] && sh_ "dumpsys SurfaceFlinger --latency '$LAYER'" > "$OUT/frames/sf-0.txt"
log "window open (logcat since $LOG_SINCE)"
END=$((SECONDS + DURATION))
i=0
while (( SECONDS < END )); do
  sleep "$POLL"
  i=$((i + 1))
  [[ -n $LAYER ]] && sh_ "dumpsys SurfaceFlinger --latency '$LAYER'" > "$OUT/frames/sf-$i.txt"
  [[ -n $APP_PID ]] && sh_ dumpsys gfxinfo "$PKG" framestats > "$OUT/frames/hwui-$i.txt"
done
cpu_snapshot "$OUT/cpu-end.txt"
log "window closed after $i polls"
[[ -n $APP_PID ]] && sh_ dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo-end.txt"
mem_snapshot end
thermal end
sh_ dumpsys cpuinfo > "$OUT/cpuinfo-end.txt"
state end

adb_ logcat -d -T "$LOG_SINCE" -s CielWallpaper:V | tr -d '\r' > "$OUT/logcat-cielwallpaper.txt" || true
adb_ logcat -d -T "$LOG_SINCE" | tr -d '\r' \
  | grep -E 'chromium|Content.Security.Policy|CSP violation|WebGL|FATAL|Uncaught|ANR in|crash' \
  | grep -E " (${APP_PID:-x}|${RENDERER_PIDS// /|}) " > "$OUT/logcat-errors.txt" || true

# --- summary ------------------------------------------------------------------
python3 -I - "$OUT" "$DURATION" "$TARGET_FPS" <<'PY' | tee "$OUT/summary.txt"
import glob, os, re, statistics, sys
out, dur, target = sys.argv[1], float(sys.argv[2]), float(sys.argv[3])
budget = 1000.0 / target
def pct(v, q):
    v = sorted(v); return v[min(len(v) - 1, int(round(q / 100 * (len(v) - 1))))] if v else float('nan')
def frames(kind, parse, after=0):
    seen = {}
    for f in sorted(glob.glob(f"{out}/frames/{kind}-*.txt"), key=lambda p: int(re.findall(r'(\d+)\.txt', p)[0])):
        if f.endswith('-0.txt'): continue
        for key, val in parse(open(f).read()):
            if key > after: seen[key] = val
    return [seen[k] for k in sorted(seen)]
def sf_parse(text):  # desired, actual present, latch (ns)
    for l in text.splitlines()[1:]:
        p = l.split()
        if len(p) == 3 and 0 < int(p[1]) < 2**62: yield int(p[1]), int(p[1])
def hwui_parse(text):
    hdr = None
    for l in text.splitlines():
        p = l.strip().rstrip(',').split(',')
        if p[0] == 'Flags': hdr = {n: i for i, n in enumerate(p)}; continue
        if hdr and len(p) == len(hdr) and p[0].isdigit() and p[0] == '0':
            iv, done = int(p[hdr['IntendedVsync']]), int(p[hdr['FrameCompleted']])
            yield iv, (iv, (done - iv) / 1e6)
def cadence(ts):
    return [(b - a) / 1e6 for a, b in zip(ts, ts[1:])]
def report(name, ts, extra=None):
    iv = cadence(ts)
    if len(ts) < 2:
        print(f"{name}: frames={len(ts)} (no delivered frames in window)"); return
    span = (ts[-1] - ts[0]) / 1e9
    print(f"{name}: frames={len(ts)} span={span:.1f}s fps_mean={(len(ts)-1)/span:.2f} "
          f"interval_ms p50={pct(iv,50):.1f} p90={pct(iv,90):.1f} p99={pct(iv,99):.1f} "
          f"late(>1.5x{budget:.1f}ms)={sum(x > 1.5*budget for x in iv)} "
          f"gaps(>100ms)={sum(x > 100 for x in iv)}" + (extra or ''))
print(f"duration={dur:.0f}s target_fps={target:.0f}")
sf0 = f"{out}/frames/sf-0.txt"
before = max((k for k, _ in sf_parse(open(sf0).read())), default=0) if os.path.exists(sf0) else 0
report("sf_presents", frames('sf', sf_parse, before))
hw = frames('hwui', hwui_parse)
rt = [d for _, d in hw]
report("hwui_frames", [t for t, _ in hw],
       f" render_ms p50={pct(rt,50):.1f} p90={pct(rt,90):.1f} p99={pct(rt,99):.1f}" if rt else '')
g = open(f"{out}/gfxinfo-end.txt").read() if os.path.exists(f"{out}/gfxinfo-end.txt") else ''
m = re.search(r'Total frames rendered: (\d+)', g); j = re.search(r'Janky frames: (\d+)', g)
if m: print(f"gfxinfo_total_frames={m.group(1)} gfxinfo_fps={int(m.group(1))/dur:.2f} "
            f"gfxinfo_janky={j.group(1) if j else '?'} (janky uses the 120 Hz panel deadline)")
def cpu(f):
    l = open(f).read().splitlines(); tot = sum(map(int, l[0].split()[1:]))
    return tot, {p[1]: int(p[2]) + int(p[3]) for p in (x.split() for x in l[1:]) if len(p) == 4}
pids = dict(l.strip().split('=', 1) for l in open(f"{out}/pids.txt") if '=' in l)
t0, a = cpu(f"{out}/cpu-start.txt"); t1, b = cpu(f"{out}/cpu-end.txt")
ncpu = os.cpu_count()
role = {}
for k in ('app', 'renderer', 'surfaceflinger', 'systemui'):
    for p in pids.get(k, '').split(): role[p] = k
parts = []
for p in b:
    if p in a: parts.append(f"{role.get(p, '?')}[{p}]={(b[p]-a[p])/dur:.1f}%")
print("cpu_percent_of_one_core (jiffies@100Hz / duration): " + ' '.join(parts))
for l in open(f"{out}/meminfo.txt") if os.path.exists(f"{out}/meminfo.txt") else []:
    m = re.match(r'pid (\d+) (\w+) .*?(\d+)', l)
    if m: print(f"pss_kb {role.get(m.group(1), '?')}[{m.group(1)}] {m.group(2)}={m.group(3)}")
th = open(f"{out}/thermal.txt").read().split('## ')[1:]
for blk in th:
    st = re.search(r'Thermal Status: (\d+)', blk); temps = re.findall(r'mValue=([\d.]+), mType=\d+, mName=(\w+)', blk)
    print(f"thermal {blk.split()[0]} status={st.group(1) if st else '?'} " + ' '.join(f"{n}={v}" for v, n in temps))
vis = [l.split('CielWallpaper: ')[1] for l in open(f"{out}/logcat-cielwallpaper.txt") if 'CielWallpaper: ' in l]
print(f"cielwallpaper_log_lines={len(vis)} last={vis[-1] if vis else '-'}")
print(f"error_lines={sum(1 for _ in open(f'{out}/logcat-errors.txt'))}")
PY
log "done"
