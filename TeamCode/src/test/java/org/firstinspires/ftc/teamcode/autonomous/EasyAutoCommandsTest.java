package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EasyAutoCommandsTest {
    private FakeFollower f;
    private EasyAuto auto;
    private double time;
    @Before public void setup() { Scheduler.reset(); f = new FakeFollower(); auto = new EasyAuto(f, 10, () -> time); }
    @After public void cleanup() { Scheduler.reset(); }
    private void at(double x, double y, double degrees) { f.pose = new Pose(x, y, Math.toRadians(degrees)); }
    private void target(double x, double y, double degrees) {
        assertEquals(x, f.target.x(), 1e-8); assertEquals(y, f.target.y(), 1e-8);
        assertEquals(0, Math.atan2(Math.sin(f.target.heading() - Math.toRadians(degrees)),
                Math.cos(f.target.heading() - Math.toRadians(degrees))), 1e-8);
    }
    private void finishLeg() {
        f.pose = f.target; f.following = false;
        Scheduler.execute(); time += .25; Scheduler.execute();
    }
    @Test public void robotDirectionsRotateWithFront() {
        at(20, 30, 90);
        Scheduler.schedule(auto.up(6)); target(20, 36, 90);
        Scheduler.schedule(auto.left(6)); target(14, 30, 90);
        Scheduler.schedule(auto.right(6)); target(26, 30, 90);
        Scheduler.schedule(auto.down(6)); target(20, 24, 90);
    }
    @Test public void directionsWorkAtAllCardinalHeadings() {
        for (int degrees = 0; degrees < 360; degrees += 90) {
            at(20, 30, degrees);
            Scheduler.schedule(auto.moveRobot(3, 4));
            double a = Math.toRadians(degrees);
            target(20 + 3*Math.cos(a)-4*Math.sin(a), 30+3*Math.sin(a)+4*Math.cos(a), degrees);
        }
    }
    @Test public void sequenceUsesPoseAtStepStart() {
        Command routine = auto.sequence(auto.up(6), auto.turnNorth(), auto.left(6));
        Scheduler.schedule(routine); target(6, 0, 0);
        finishLeg(); target(6, 0, 90);
        finishLeg(); target(0, 0, 90);
        finishLeg(); assertFalse(Scheduler.isScheduled(routine));
    }
    @Test public void cardinalTargetsAreAbsolute() {
        at(20,30,37);
        Scheduler.schedule(auto.turnNorth()); target(20,30,90);
        Scheduler.schedule(auto.turnSouth()); target(20,30,270);
        Scheduler.schedule(auto.turnWest()); target(20,30,180);
        Scheduler.schedule(auto.turnEast()); target(20,30,0);
    }
    @Test public void relativeTurnPreservesSignAndFullRevolution() {
        at(20,30,350);
        Command turn = auto.turnByDegrees(360); Scheduler.schedule(turn);
        target(20,30,80);
        f.pose=f.target; Scheduler.execute(); target(20,30,170);
        f.pose=f.target; Scheduler.execute(); target(20,30,260);
        f.pose=f.target; Scheduler.execute(); target(20,30,350);
        assertTrue(Scheduler.isScheduled(turn)); finishLeg(); assertFalse(Scheduler.isScheduled(turn));
        Scheduler.schedule(auto.turnByDegrees(-270)); target(20,30,260);
        f.pose=f.target; Scheduler.execute(); target(20,30,170);
        f.pose=f.target; Scheduler.execute(); target(20,30,80);
    }
    @Test public void fieldMoveAndCoordinateMoveIgnoreRobotHeading() {
        at(20,30,90); Scheduler.schedule(auto.moveField(6,4)); target(26,34,90);
        Scheduler.schedule(auto.goTo(50,60)); target(50,60,90);
    }
    @Test public void zeroMoveDoesNotCreateDegeneratePath() {
        Scheduler.schedule(auto.up(0)); assertEquals(0, f.pathsStarted); target(0,0,0);
    }
    @Test public void stoppedFollowerFarFromTargetIsNotSuccess() {
        Command c=auto.up(6); Scheduler.schedule(c); f.following=false; f.busy=false;
        Scheduler.execute(); time=1; Scheduler.execute(); assertTrue(Scheduler.isScheduled(c));
    }
    @Test public void timeoutStopsAndDoesNotStartNextStep() {
        Command c=auto.sequence(auto.up(6),auto.left(6)); Scheduler.schedule(c); time=11;
        try { Scheduler.execute(); fail("Expected motion failure"); }
        catch (EasyAuto.MotionFailedException expected) { assertTrue(f.drive.stopped); }
        assertEquals(1,f.pathsStarted); Scheduler.cancel(c);
    }
    @Test public void cancellationStopsImmediately() {
        Command c=auto.up(6); Scheduler.schedule(c); Scheduler.cancel(c);
        assertTrue(f.drive.stopped); assertFalse(f.following);
    }
    @Test public void invalidPoseStops() {
        at(Double.NaN,0,0);
        try { Scheduler.schedule(auto.up(6)); fail("Expected invalid pose failure"); }
        catch (EasyAuto.MotionFailedException expected) { assertTrue(f.drive.stopped); }
    }
    @Test public void rejectsInvalidArguments() {
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try { auto.up(value); fail("Expected invalid argument"); }
            catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void pauseStopsAndWaits() {
        Command c=auto.pause(1); Scheduler.schedule(c); assertTrue(f.drive.stopped);
        time=.5; Scheduler.execute(); assertTrue(Scheduler.isScheduled(c));
        time=1; Scheduler.execute(); assertFalse(Scheduler.isScheduled(c));
    }
    private static final class FakeFollower extends Follower {
        final FakeDrive drive;
        boolean following, busy, parametricEnd;
        int pathsStarted;
        Pose pose = new Pose(0, 0, 0), target;
        @Override public Pose pose() { return pose; }
        @Override public void hold(Pose target) { this.target = target; following = false; }
        FakeFollower() { this(new FakeDrive()); }
        private FakeFollower(FakeDrive drive) { super(null, drive, null); this.drive = drive; }
        @Override public void follow(Path path) {
            target = path.endPose(); pathsStarted++; following = true; busy = true; parametricEnd = false;
        }
        @Override public boolean following() { return following; }
        @Override public boolean isBusy() { return busy; }
        @Override public boolean atParametricEnd() { return parametricEnd; }
        @Override public void stop() { following = false; }
    }

    private static final class FakeDrive implements Drivetrain {
        boolean stopped;
        public void drive(DrivePowers powers, boolean manual) { }
        public double maxScaling(DrivePowers current, DrivePowers delta) { return 1; }
        public void stop() { stopped = true; }
        public void stop(boolean brake) { stop(); }
        public Map<String, Object> debug() { return Collections.emptyMap(); }
        public double interpolateVelocity(double x, double y, double theta) { return 1; }
    }
}
