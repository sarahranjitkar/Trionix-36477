package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.paths.Path;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

public final class AutoCommands {
    private AutoCommands() { }

    public static Command followAndSettle(Follower follower, Path path) {
        // Ivy 1.1.1 defaults to parametric completion. Wait for Pedro's endpoint
        // conditions/timeout as well before starting the next action.
        return follow(follower, path)
                .setDone(() -> !follower.following() && !follower.isBusy())
                .requiring(follower)
                .setEnd(reason -> {
                    if (reason != EndCondition.NATURALLY) stop(follower);
                });
    }

    public static void stop(Follower follower) {
        follower.stop();
        // Follower.stop changes mode; directly zero motors without another loop.
        follower.drivetrain.stop();
    }
}
