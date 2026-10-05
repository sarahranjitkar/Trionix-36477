# Launcher 2400 RPM burst tuning

Use **Launcher Burst Tuning** to compare fast spin-up with recovery during a manually loaded four-POLLEN burst. It controls the existing single `Launcher` motor, intake, and calibrated `Servo Gate`. The drive motors are not commanded. The gate uses your current saved endpoints, 0.44 closed and 0.65 open; confirm they still match the mechanism.

## Changing the target RPM

The default target is **2400 RPM**, adjustable between trials from **1500 to 3000 RPM**. Use either bumper to cycle through P, I, D, F, and **Target RPM**. With Target RPM selected, D-pad LEFT/RIGHT changes it by **100 RPM per press**. Right-stick click still changes only the PIDF gain step. Press A to start at the selected target. Target edits do not spin the motor and are ignored during an active trial.

Telemetry separates **Target RPM (next run)** from **Results target RPM**. Each trial captures its target; readiness, errors, suggestions, and CSV files use that captured value even after selecting a different target. Readiness is selected target +/-75 RPM (2325–2475 at 2400). Old-target suggestions cannot be accepted after changing the target; first run another test. The existing 3200 RPM abort threshold is retained, so target selection is limited to 3000 RPM.

Controller target/PIDF edits are session-only. Record them before restarting or redeploying. Recheck F after a target change because the feedforward command scales with the target. The examples below use the default 2400; substitute your selected target.

## Start with a baseline then retune in stages

Recommendation: preserve one set of results with the current gains, then rebuild the tune with feedforward first, proportional feedback second, and integral/derivative only if tests justify them. The current numbers are not proven wrong merely because recovery is slow. This process separates an inadequate baseline drive command from weak feedback or a mechanical/power limit. Do not set all four gains to zero and expect the wheel to run.

### Step 1 Prepare consistent conditions

Use the same launcher geometry and a healthy, charged battery. Check that the wheels turn freely with power off, belts do not slip, and the encoder reading is plausible. Keep the launcher empty for the first stages. Use the same battery conditions across comparisons; the logs record voltage, but do not normalize different trials to equal voltage. Confirm the gate endpoints before a loaded trial.

### Step 2 Record the current baseline

Select this OpMode, press INIT and START, then press X until telemetry says SPIN-UP ONLY. The tuner now starts at the team-requested P=54.087, I=0, D=0, F=14.345. Record those as your baseline, or manually enter the earlier P=4.8, I=0.32, D=0.2, F=5.0 if you specifically want to reproduce the old baseline. Press A. Record settling time, peak RPM and baseline mean/range. Repeat three times, allowing the wheel to coast below 100 RPM before each A press. Retain the CSV files, including failed runs. If it overspeeds or behaves erratically, abort rather than completing three trials.

If unloaded behavior is stable, record up to three current-gain four-POLLEN bursts as a loaded reference too. Verify all four leave within the selected feed window. If unloaded behavior is unstable, skip this loaded baseline and proceed with the unloaded retune.

### Step 3 Isolate feedforward

Return to SPIN-UP ONLY with no POLLEN. Between runs, use the bumpers to select each gain, D-pad LEFT to reduce it, and right-stick click to choose adjustment size. Set:

| Gain | Starting value for this stage |
| --- | --- |
| P | 0 |
| I | 0 |
| D | 0 |
| F | 14.345, the current tuning default used only as an initial trial |

These are experimental starting values, not recommended final settings. With feedback terms at zero, F supplies the baseline drive command. Keep this stage unloaded. Do not press Y to accept automatic suggestions during this stage: those suggestions assume a combined controller and may suggest adding P before you have finished isolating F.

### Step 4 Find an F that holds close to 2400 unloaded

Press A and observe the RPM trace. If speed levels off below 2400, increase F by **0.5** for the next trial (five RIGHT taps with step 0.1). If it levels off above 2400, decrease F by 0.5. When near target, use changes of **0.1**, then **0.01** if useful. Change only F.

An F-only trial may time out after eight seconds without entering the readiness band; this is usable data and the gate stays closed. In that case, examine the last second of RPM samples in the CSV, since baseline mean may be NaN. Do not interpret a still-rising trace as a settled speed. If it never levels off, behaves erratically, or hits a guardrail, investigate the mechanism/encoder before continuing. A low initial F can require several trials; do not jump directly to a large number.

The aim is a reasonably steady unloaded speed near 2400, ideally within the tuner's +/-75 RPM band. Do not tune F to make spin-up maximally aggressive or to compensate for all four balls at once. Some battery-dependent error is expected with feedforward alone.

### Step 5 Add P for fast settling

Keep the selected F fixed and I=D=0. Start P at **0.5**, then increase in **0.5** increments between trials. These are proposed search increments, not manufacturer-prescribed gains. Use 0.1 increments near a promising setting. Compare complete settling time and peak RPM, not merely the first crossing of 2400.

Stop increasing when overshoot, repeated speed oscillation, or settling time worsens. Return to the previous better value and refine around it. It is not necessary to deliberately provoke sustained oscillation. Run three repeats of promising candidates. Keep a P/F pair that settles quickly and holds without repeated overshoot; if P changes expose a persistent unloaded bias, make a small F correction in a separate trial.

### Step 6 Establish the four-POLLEN feed window

With motors stopped, load four POLLEN. Use X to select FOUR-POLLEN TIMED BURST. Begin with feed power 0.50 and duration 2.0 seconds, then press A. Confirm physically whether all four clear the gate and launcher. Adjust duration using D-pad UP/DOWN if needed. Do not count an apparently excellent RPM result as successful if one or more balls remained inside.

Once the duration reliably passes all four, freeze both feed duration and intake power for PIDF comparisons. Note them beside each result. If the required duration exceeds the tuner's six-second limit, inspect feeding rather than assuming a PIDF problem.

### Step 7 Improve loaded recovery using P

Run three bursts at the selected P/F pair with I=D=0. Record minimum feed RPM, feed RMS error, percentage of feed time in band, post-feed recovery, and whether all four launched successfully. Increase P by about **10%** as a trial, keeping F, feed settings and loading the same. Enter the value manually using the gain controls, or press Y only if the displayed suggestion matches this experiment.

Keep the change only if repeated loaded results improve without unacceptable overshoot or worse unloaded settling. Restore the previous value if performance worsens. Repeat in small increments until improvement stops. An instantaneous speed dip when a ball takes energy from the wheel is expected; prioritize consistent launch performance and recovery, not a perfectly flat trace at any cost.

### Step 8 Add I only for persistent error

Leave I at zero if P/F already works well. If the wheel remains systematically below target over a sustained interval despite reasonable F/P, try I=**0.01**, then changes of 0.01. Run both unloaded and burst tests after each change. These small values are exploratory steps, not a promise of adequate correction.

Reduce/remove I if the wheel shoots above target after the fourth ball, settles more slowly, or develops slow oscillations. Integral action is not the first remedy for a short impact-related RPM dip, and cannot provide power beyond what the motor/battery can supply.

### Step 9 Add D only if a clear problem remains

Keep D=0 unless the tuned P/F (and any necessary I) still produces overshoot or oscillation that testing shows D can improve. Try D=**0.01** and compare repeated trials before increasing in small steps. If the RPM becomes noisier or settling/recovery worsens, return D to zero. A successful tune does not need every gain to be nonzero.

### Step 10 Choose a repeatable winner

For each finalist, run at least three unloaded starts and three four-POLLEN bursts. Compare the median and worst result, not only the fastest run. Record: P/I/D/F, feed time/power, battery voltage, settled time, peak RPM, minimum feed RPM, feed RMS error, post-feed recovery, and whether all four balls actually launched. Recheck the best candidate at a lower normal operating battery charge.

The current +/-75 RPM readiness band and 0.30-second dwell are test criteria, not proof of sufficient shot accuracy. Judge launch consistency too. Do not loosen readiness or slow feeding merely to make the figures look better; treat feed-rate changes as a separate experiment. If stronger feedback stops helping, check voltage sag, ball compression, wheel/belt slip, binding, and available motor torque. These logs cannot establish actual output saturation.

### Step 11 Save and verify

Copy the winning values into `Flywheel.TUNED_PIDF`, redeploy, and run the same validation again. Until then, controller edits affect only this tuning session. Preserve the old baseline and winning logs for comparison. No final PIDF is selected until the hardware results support it.

The staged procedure above is an experimental recommendation for this robot. The roles of feedforward and feedback are also discussed in [REV's FTC flywheel programming guidance](https://docs.revrobotics.com/ftc-kickoff-concepts/decode-2025-26/programming-tips-and-tricks); gains from other robots/controllers are not interchangeable with these Hub settings.

## Run a repeatable test

1. Deploy this checkout, select **Launcher Burst Tuning**, and INIT. INIT stops the launcher/intake and commands the gate CLOSED. Keep the launch area clear and point into a collection area. Load four POLLEN manually only while the launcher and intake are stopped.
2. Press START. Initial tuning gains are P=54.087, I=0, D=0, F=14.345. The tuner reads these defaults from `Flywheel.TUNED_PIDF`, which also applies them to the normal launcher. The target defaults to 2400 motor-shaft RPM and can be adjusted between trials. Feed defaults to 2 seconds at 0.50 power, matching the team-approved burst settings. Normal driver-controlled intake power is configured separately. Confirm all four POLLEN clear with the current load and battery.
3. For goal 1, press X to select SPIN-UP ONLY. Press A after START. The wheel must be below 100 RPM to start a comparable run. The motor accelerates to 2400; the gate remains closed throughout this mode.
4. The test records the first reading within 2400 +/-75 RPM, then requires 0.30 seconds continuously in that band. This defines "settled". It observes an additional continuously in-band second before progressing. The measurement avoids triggering feed on a momentary crossing of 2400. The readiness delay is deliberate and separate from the reported settling time.
5. For goal 2, select FOUR-POLLEN TIMED BURST with X and load four POLLEN. Press A. After the same readiness check, the gate opens, waits 0.25 seconds for travel, then the intake runs for the selected duration. The launcher holds the same 2400 RPM command throughout. The test does not pause intake for ordinary RPM dips, so it measures the genuine burst load.
6. At the end of the feed window, the intake stops and the gate closes. The launcher stays at 2400 while recovery is measured, for at least one second and at most four seconds. It then stops automatically. B or BACK aborts immediately, stops both motors, and commands the gate closed.
7. Read the results and recommendation. Wait for the wheel to coast below 100 RPM, reload the same four POLLEN, change one gain, and repeat. Keep battery charge, feed duration/power, ball loading, compression, and mechanism geometry consistent. Do not optimize by slowing the intake unless you deliberately start a separate feed-rate experiment.

The gate/ball passage has no sensor. The software cannot count four shots, confirm the gate physically arrived, or distinguish individual balls from RPM dip episodes. Verify all four balls actually clear within the chosen feed window. Increase feed duration if needed, then keep it fixed for PIDF comparisons. The quarter-second gate travel allowance should also be checked physically.

## Controller controls

All adjustments are accepted **between runs only**. Each tap changes the setting once; gain changes cannot contaminate an active trial.

| Gamepad 1 | Action |
| --- | --- |
| A | Start the selected trial |
| B or BACK | Abort; stop motors and command gate closed |
| X | Toggle spin-up-only / four-POLLEN burst |
| Left / right bumper | Select P, I, D, F, or Target RPM |
| D-pad left / right | Decrease / increase selected gain, or target RPM by 100 |
| Right stick click | Cycle gain step: 0.01, 0.1, 1.0 |
| D-pad down / up | Feed time -/+0.25 seconds, limited to 0.5–6 seconds |
| Left / right trigger | Feed power -/+0.05, limited to 0.10–1.00 |
| Y | Select the last trial's suggested single-gain change, once per trial |

Y does not start the motor. The selected values apply on the next A press. Controller edits are temporary. To use a successful result in match code, copy the chosen P/I/D/F into `Flywheel.TUNED_PIDF` and redeploy. The log retains the exact gains used in each run. Hub PIDF changes are not persistent through power cycles ([FTC documentation](https://ftc-docs.firstinspires.org/en/latest/programming_resources/shared/pidf_coefficients/pidf-coefficients.html)).

## Read the results

- **First band time:** first arrival within +/-75 RPM. A fast crossing alone is not good settling.
- **Settled time:** first continuously in-band 0.30-second interval completed, measured from the run's velocity command.
- **Spin-up peak:** maximum RPM before opening the gate, including the baseline interval.
- **Baseline mean/range:** unloaded speed after initial settling and before gate opening. A large range suggests unstable holding.
- **Minimum feed RPM:** lowest sampled speed while the intake runs.
- **Feed RMS error and mean absolute error:** time-weighted deviation from 2400 during feeding. Lower is better with the same feed/load settings.
- **Feed in-band percentage:** proportion of observed feed time within +/-75 RPM. Higher is better.
- **Post-feed recovery:** time after intake-off until the first completed 0.30-second in-band interval. Its minimum is about 0.30 seconds, even if there was no dip.
- **Recovered dip duration:** below-band excursion until stable in-band return. Dips can merge across multiple balls, and are not shot counts. An unresolved dip is reported separately instead of being assigned a misleading zero recovery time.

NaN means not measured/not reached, including feed statistics for a spin-up-only run. Compare several repeated runs per setting, not a single lucky trial. Prefer low settling time, modest overshoot, low loaded RPM error, and fast recovery together. This tuner supports one PIDF set for the whole trial; it does not switch gains between spin-up and feeding.

## How suggestions work

Recommendations are conservative experiments, not a calculated optimal PIDF:

- More than 150 RPM overshoot or baseline range: try reducing I by 20% if I is nonzero; otherwise reduce P by 20%.
- Settled unloaded mean more than 35 RPM low/high: try increasing/decreasing F by 5%.
- Settling slower than 2 seconds, a loaded dip over 150 RPM, post-feed recovery over 0.8 seconds, or an unresolved dip: try increasing P by 10%.
- Invalid samples, operator stops, severe stalls, and no successful initial settling generally need diagnosis rather than a blind increase. D remains manually adjustable; noisy encoder data makes an automatic D recommendation unreliable.

Only one suggestion is made per run, in the priority order above. If a P increase worsens overshoot/error, restore the previous value. Excessive I can accumulate during a burst and cause overshoot after the balls clear. PIDF cannot overcome insufficient motor torque, battery sag, a jam, wheel slip, or excessive launcher compression. Current and voltage are logged to help investigate; the FTC velocity API used here does not expose actual controller output saturation, so the tool does not claim to detect it.

## Log files and timing

Logs are created on the Control Hub in `AppUtil.ROBOT_DATA_DIR/launcher-tuning`; the resolved path appears during INIT (normally `/sdcard/FIRST/data/launcher-tuning`). Each run gets a unique timestamp/ID and two CSV files:

- `trial-...-samples.csv`: monotonic elapsed time, observed/next phase, selected RPM reference, measured RPM/error, launcher amps, battery voltage, commanded gate/intake outputs, and the run's PIDF.
- `trial-...-summary.csv`: mode, outcome, timing, errors/recovery, feed settings, gains, and recommendation.

The reference column retains the trial target in the final sample for comparison; DONE/ABORTED means motor power was removed. Observed phase describes the state at sampling; next phase and output columns describe commands after processing that sample. Data is buffered in RAM during motion and written after motor stop. A sudden power loss/app crash can therefore lose that run's buffered samples. Normal completion, abort, and Driver Station STOP attempt to save partial results. A log creation failure prevents starting a trial; a write failure is reported rather than claiming a saved result.

Use Android Studio Device Explorer or `adb pull /sdcard/FIRST/data/launcher-tuning` to copy logs to your computer (substitute the displayed path if different). Save fetched working logs under `~/tmp/trionix`. Plot elapsed seconds versus RPM, then compare summaries for identical loads. Send the CSV files for a more specific tuning recommendation.

Sampling requests roughly 20 ms sleeps plus hardware/loop overhead; actual elapsed times are logged. A gap over 250 ms aborts because continuity cannot be verified. The tuner also stops on spin-up timeout (8 seconds), unstable readiness (12 seconds total), reversed/over-3200 RPM readings, RPM below 1000 for 250 ms while feeding, or recovery timeout (4 seconds). These are software guardrails, not jam/current protection. STOP commands closure but cannot guarantee the gate finishes moving after servo power is removed.
