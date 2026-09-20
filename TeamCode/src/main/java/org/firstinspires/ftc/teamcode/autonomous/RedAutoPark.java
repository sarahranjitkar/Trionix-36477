package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.commands.groups.SequentialGroup;
import dev.nextftc.extensions.pedro.FollowPath;
import dev.nextftc.extensions.pedro.PedroComponent;
import dev.nextftc.ftc.NextFTCOpMode;
import dev.nextftc.ftc.components.BulkReadComponent;
import static dev.nextftc.extensions.pedro.PedroComponent.follower;


@Autonomous (name ="Red Park Auto")
public class RedAutoPark extends NextFTCOpMode {
    public static Pose startPose = new Pose(100, 8, Math.toRadians(90));
    public static Pose secondPose = new Pose(100, 30, Math.toRadians(90));
    public static Pose parkPose = new Pose(47,8, Math.toRadians(90));
    public RedAutoPark() {
        addComponents(
                new PedroComponent(Constants::createFollower),
                BulkReadComponent.INSTANCE
        );
    }

    private PathChain startToUpPath, parkPath;

    public void buildPaths() {
        startToUpPath = follower().pathBuilder()
                .addPath(new BezierLine(startPose, secondPose))
                .setLinearHeadingInterpolation(startPose.getHeading(), secondPose.getHeading())
                .build();
        parkPath = follower().pathBuilder()
                .addPath(new BezierLine(secondPose, parkPose))
                .setLinearHeadingInterpolation(secondPose.getHeading(), parkPose.getHeading())
                .build();
    }

    public Command autonomousRoutine() {
        return new SequentialGroup(
                new FollowPath(startToUpPath),
                new FollowPath(parkPath)
        );
    }

    @Override
    public void onStartButtonPressed() {
        buildPaths();
        autonomousRoutine().schedule();
    }
}
