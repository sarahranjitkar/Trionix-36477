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
import static com.pedropathing.api.Paths.line;
import static com.pedropathing.ivy.groups.Groups.sequential;
import static org.junit.Assert.*;

public class AutoCommandsTest {
    private final Path first = line(new Pose(0, 0, 0), new Pose(24, 0, 0));
    private final Path second = line(new Pose(24, 0, 0), new Pose(24, 24, 0));
    private FakeFollower follower;

    @Before public void setUp() { Scheduler.reset(); follower = new FakeFollower(); }
    @After public void tearDown() { Scheduler.reset(); }

    @Test public void waitsForEndpointSettlingBeforeStartingNextPath() {
        Command routine = sequential(AutoCommands.followAndSettle(follower, first),
                AutoCommands.followAndSettle(follower, second));
        Scheduler.schedule(routine);
        assertEquals(1, follower.pathsStarted);
        follower.parametricEnd = true;
        Scheduler.execute();
        assertEquals(1, follower.pathsStarted);
        // Pedro transitions from following to holding before endpoint correction ends.
        follower.following = false;
        Scheduler.execute();
        assertEquals(1, follower.pathsStarted);
        follower.busy = false;
        Scheduler.execute();
        assertEquals(2, follower.pathsStarted);
        assertTrue(Scheduler.isScheduled(routine));
    }

    @Test public void cancellingSequenceImmediatelyZerosDrive() {
        Command routine = sequential(AutoCommands.followAndSettle(follower, first),
                AutoCommands.followAndSettle(follower, second));
        Scheduler.schedule(routine);
        Scheduler.cancel(routine);
        assertFalse(Scheduler.isScheduled(routine));
        assertFalse(follower.following);
        assertTrue(follower.drive.stopped);
        assertEquals(1, follower.pathsStarted);
    }

    @Test public void naturalCompletionPreservesEndpointHold() {
        Command command = AutoCommands.followAndSettle(follower, first);
        Scheduler.schedule(command);
        follower.following = false;
        follower.busy = false;
        Scheduler.execute();
        assertFalse(Scheduler.isScheduled(command));
        assertFalse(follower.drive.stopped);
    }

    @Test public void competingDriveCommandCancelsExistingPath() {
        Command firstCommand = AutoCommands.followAndSettle(follower, first);
        Command secondCommand = AutoCommands.followAndSettle(follower, second);
        Scheduler.schedule(firstCommand);
        Scheduler.schedule(secondCommand);
        assertFalse(Scheduler.isScheduled(firstCommand));
        assertTrue(Scheduler.isScheduled(secondCommand));
        assertTrue(follower.drive.stopped);
        assertEquals(2, follower.pathsStarted);
    }

    private static final class FakeFollower extends Follower {
        final FakeDrive drive;
        boolean following, busy, parametricEnd;
        int pathsStarted;
        FakeFollower() { this(new FakeDrive()); }
        private FakeFollower(FakeDrive drive) { super(null, drive, null); this.drive = drive; }
        @Override public void follow(Path path) {
            pathsStarted++; following = true; busy = true; parametricEnd = false;
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
