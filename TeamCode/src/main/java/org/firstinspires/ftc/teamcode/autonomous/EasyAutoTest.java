package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Interactive single-action test; START alone never moves the robot. */
@Autonomous(name = "Easy Auto Utility Test", group = "TRIONIX Tests")
public class EasyAutoTest extends LinearOpMode {
    private static final String[] ACTIONS = {"Up / forward", "Down / backward", "Left", "Right",
            "Turn North (90)", "Turn South (270)", "Turn West (180)", "Turn East (0)",
            "Turn CCW by angle", "Turn CW by angle", "Field East", "Field North",
            "Go to start (72,72)", "Pause 1 second"};

    @Override public void runOpMode() throws InterruptedException {
        Scheduler.reset();
        if (!Constants.isTuned()) {
            telemetry.addLine("Run Foresight AutoTune and save Constants first.");
            telemetry.update(); waitForStart(); return;
        }
        Follower follower = Constants.create(hardwareMap);
        EasyAuto auto = new EasyAuto(follower);
        Command active = null;
        int selection = 0;
        double inches = 6, angle = 45;
        boolean oldLeft = false, oldRight = false, oldUp = false, oldDown = false;
        boolean oldA = true, oldLb = false, oldRb = false;
        String status = "Ready";
        try {
            follower.setPose(new Pose(72, 72, 0));
            while (opModeInInit()) {
                follower.update();
                telemetry.addLine("Place at field (72,72), intake facing East (+X).");
                telemetry.addLine("Keep robot still during Pinpoint initialization.");
                telemetry.addLine("START enables menu; release A then press A to run.");
                telemetry.addData("Pose", follower.pose());
                telemetry.update(); idle();
            }
            if (isStopRequested()) return;
            while (opModeIsActive()) {
                boolean running = active != null && Scheduler.isScheduled(active);
                // B has priority over every other input and stops motors immediately.
                if (gamepad1.b) {
                    if (active != null) Scheduler.cancel(active);
                    auto.stop(); status = "Cancelled"; running = false;
                }
                if (!running && !gamepad1.b) {
                    if (gamepad1.dpad_left && !oldLeft)
                        selection = (selection + ACTIONS.length - 1) % ACTIONS.length;
                    if (gamepad1.dpad_right && !oldRight) selection = (selection + 1) % ACTIONS.length;
                    if (gamepad1.dpad_up && !oldUp) inches = Math.min(48, inches + 1);
                    if (gamepad1.dpad_down && !oldDown) inches = Math.max(1, inches - 1);
                    if (gamepad1.right_bumper && !oldRb) angle = Math.min(360, angle + 15);
                    if (gamepad1.left_bumper && !oldLb) angle = Math.max(15, angle - 15);
                }
                try {
                    follower.update();
                    if (!running && !gamepad1.b && gamepad1.a && !oldA) {
                        active = action(auto, selection, inches, angle);
                        Scheduler.schedule(active); status = "Running: " + ACTIONS[selection];
                    }
                    Scheduler.execute();
                    if (active != null && !Scheduler.isScheduled(active) && status.startsWith("Running"))
                        status = "Completed within tolerance";
                } catch (EasyAuto.MotionFailedException failure) {
                    if (active != null) Scheduler.cancel(active);
                    auto.stop(); status = failure.getMessage();
                }
                oldLeft = gamepad1.dpad_left; oldRight = gamepad1.dpad_right;
                oldUp = gamepad1.dpad_up; oldDown = gamepad1.dpad_down;
                oldLb = gamepad1.left_bumper; oldRb = gamepad1.right_bumper; oldA = gamepad1.a;
                telemetry.addData("Selected", ACTIONS[selection]);
                telemetry.addData("Distance / angle", "%.0f inches / %.0f degrees", inches, angle);
                telemetry.addLine("D-pad L/R: action | U/D: inches | bumpers: angle");
                telemetry.addLine("A: run once | B: cancel | STOP: exit");
                telemetry.addData("Status", status);
                telemetry.addData("Pose (inches/radians)", follower.pose());
                telemetry.update(); idle();
            }
        } finally {
            if (active != null) Scheduler.cancel(active);
            Scheduler.reset(); auto.stop();
        }
    }

    private Command action(EasyAuto a, int selected, double inches, double angle) {
        switch (selected) {
            case 0: return a.up(inches);
            case 1: return a.down(inches);
            case 2: return a.left(inches);
            case 3: return a.right(inches);
            case 4: return a.turnNorth();
            case 5: return a.turnSouth();
            case 6: return a.turnWest();
            case 7: return a.turnEast();
            case 8: return a.turnByDegrees(angle);
            case 9: return a.turnByDegrees(-angle);
            case 10: return a.moveField(inches, 0);
            case 11: return a.moveField(0, inches);
            case 12: return a.goTo(72, 72);
            default: return a.pause(1);
        }
    }
}
