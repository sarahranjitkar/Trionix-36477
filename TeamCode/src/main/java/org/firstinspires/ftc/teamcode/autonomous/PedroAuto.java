package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.pedro.Constants;

public abstract class PedroAuto extends LinearOpMode {
    protected abstract Pose startPose();
    protected abstract Command routine(Follower follower);

    @Override
    public final void runOpMode() throws InterruptedException {
        Scheduler.reset();
        if (!Constants.isTuned()) {
            telemetry.addLine("Autonomous unavailable: run Foresight AutoTune first.");
            telemetry.addLine("Paste its Java configuration into pedro/Constants.java and rebuild.");
            telemetry.update();
            waitForStart();
            return;
        }
        Follower follower = Constants.create(hardwareMap);
        Command routine = null;
        try {
            follower.setPose(startPose());
            routine = routine(follower);
            while (opModeInInit()) {
                follower.update();
                telemetry.addData("Start pose (inches/radians)", follower.pose());
                telemetry.update();
                idle();
            }
            if (isStopRequested()) return;
            Scheduler.schedule(routine);
            while (opModeIsActive()) {
                follower.update();
                Scheduler.execute();
                telemetry.addData("Pose", follower.pose());
                telemetry.addData("Routine running", Scheduler.isScheduled(routine));
                telemetry.update();
                idle();
            }
        } finally {
            if (routine != null) Scheduler.cancel(routine);
            Scheduler.reset();
            AutoCommands.stop(follower);
        }
    }
}
