package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import static com.pedropathing.api.Paths.line;
import static org.firstinspires.ftc.teamcode.autonomous.AutoCommands.followAndSettle;

/** Short first path: place the robot at the documented pose in clear space. */
@Autonomous(name = "24 Inch Movement Test", group = "TRIONIX Tests")
public class MovementTest extends PedroAuto {
    private static final PoseFactory P = PoseFactory.degrees();
    private final Pose start = P.of(24, 24, 0);
    private final Pose end = P.of(48, 24, 0);

    @Override protected Pose startPose() { return start; }
    @Override protected Command routine(Follower follower) {
        return followAndSettle(follower, line(start, end).linear(start, end));
    }
}
