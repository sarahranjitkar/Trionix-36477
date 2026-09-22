package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.ForesightTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.PinpointTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.Tests;

public final class Tuning {
    @Tuner
    public static Procedure mecanumTuner() { return new MecanumTuner(); }

    @Tuner
    public static Procedure pinpointTuner() { return new PinpointTuner(); }

    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(Constants::createLocalizer, Constants::createDrivetrain);
    }

    @Tuner
    public static Procedure tests() {
        return new Tests(Constants::createDrivetrain, Constants::createLocalizer,
                Constants.isTuned() ? () -> new Foresight(Constants.foresightConfig) : null);
    }
}
