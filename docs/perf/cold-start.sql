-- Ritune cold-start summary for a trace recorded with docs/perf/cold-start.pftxt.
-- All times are in ms from the moment Android started launching the app.
--
--   trace_processor query -f docs/perf/cold-start.sql <trace>
--
-- Ritune's main thread is found by thread name, not process name: on some phones
-- (the Redmi) the process is still labelled zygote64 when the trace starts.

INCLUDE PERFETTO MODULE android.startup.startups;

-- 1. Milestones: the lifecycle, Ritune's sections, TTID and TTFD.
SELECT
  s.name,
  ROUND((s.ts - launch.ts) / 1e6, 1)         AS start_ms,
  ROUND((s.ts + s.dur - launch.ts) / 1e6, 1) AS end_ms,
  ROUND(s.dur / 1e6, 1)                      AS dur_ms
FROM slice AS s,
     (SELECT ts FROM android_startups WHERE package = 'com.ritune') AS launch
WHERE s.name IN (
    'bindApplication', 'activityStart', 'activityResume',
    'ThemeRead', 'InitializeApp', 'Seed.anchors', 'Seed.routines'
  )
UNION ALL
SELECT 'TTID (startup end)', ROUND(dur / 1e6, 1), NULL, NULL
FROM android_startups WHERE package = 'com.ritune'
UNION ALL
SELECT
  'TTFD (reportFullyDrawn)',
  ROUND((MIN(s.ts) - launch.ts) / 1e6, 1), NULL, NULL
FROM slice AS s,
     (SELECT ts FROM android_startups WHERE package = 'com.ritune') AS launch
WHERE s.name GLOB 'reportFullyDrawn() for *'
ORDER BY start_ms;

-- 2. Frames on the main thread up to TTFD, with missed deadlines at 60 Hz and 120 Hz.
--    missed = ceil(frame time / frame period) - 1. Use the column for the phone's refresh rate.
SELECT
  ROUND((s.ts - launch.ts) / 1e6, 1) AS start_ms,
  ROUND(s.dur / 1e6, 1)              AS dur_ms,
  MAX(CAST((s.dur + 16666666) / 16666667 AS INT) - 1, 0) AS missed_60hz,
  MAX(CAST((s.dur + 8333332) / 8333333 AS INT) - 1, 0)   AS missed_120hz,
  s.name
FROM slice AS s
JOIN thread_track AS tt ON s.track_id = tt.id
JOIN thread       AS t  ON tt.utid    = t.utid
JOIN (SELECT ts FROM android_startups WHERE package = 'com.ritune') AS launch
WHERE t.name = 'com.ritune'
  AND t.is_main_thread = 1
  AND s.name GLOB 'Choreographer#doFrame*'
  AND s.depth = 0  -- newer Android nests a 'doFrame - resynced' slice inside each frame
  AND s.ts <= (SELECT MIN(ts) FROM slice WHERE name GLOB 'reportFullyDrawn() for *')
ORDER BY start_ms;
