package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.commands.groups.SequentialGroup;
import dev.nextftc.extensions.pedro.FollowPath;
import dev.nextftc.extensions.pedro.PedroComponent;
import static dev.nextftc.extensions.pedro.PedroComponent.follower;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

import dev.nextftc.ftc.NextFTCOpMode;
import dev.nextftc.ftc.components.BulkReadComponent;

@Autonomous (name = "Nemtomonomous")
public class Simple_Movement extends NextFTCOpMode {
    public static Pose startPose = new Pose(56,12,Math.toRadians(90));
    public static Pose secondPose = new Pose(56,114,Math.toRadians(135));
    public static Pose thirdPose = new Pose(90,32,Math.toRadians(90));
    public static Pose lastPose = new Pose(90,114,Math.toRadians(90));
    public Simple_Movement() {
        addComponents(
                new PedroComponent(Constants::createFollower),
                BulkReadComponent.INSTANCE
        );
    };
    private PathChain initialToBottomStart, secondPath, lastPath;
    public void buildPaths() {
        initialToBottomStart = follower().pathBuilder()
                .addPath(new BezierLine(startPose, secondPose))
                .setLinearHeadingInterpolation(startPose.getHeading(), secondPose.getHeading())
                .build();
        secondPath = follower().pathBuilder()
                .addPath(new BezierLine(secondPose, thirdPose))
                .setLinearHeadingInterpolation(secondPose.getHeading(), thirdPose.getHeading())
                .build();
        lastPath = follower().pathBuilder()
                .addPath(new BezierLine(thirdPose, lastPose))
                .setLinearHeadingInterpolation(thirdPose.getHeading(), lastPose.getHeading())
                .build();
    }
    public Command autonoumousRoutine() {
        return new SequentialGroup (
                new FollowPath(initialToBottomStart),
                new FollowPath(secondPath),
                new FollowPath(lastPath)
        );
    }
    @Override
    public void onStartButtonPressed() {
        buildPaths();
        autonoumousRoutine().schedule();
    }
}