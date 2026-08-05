package org.firstinspires.ftc.teamcode;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.subsystems.Subsystem;
import dev.nextftc.hardware.impl.MotorEx;
import dev.nextftc.hardware.powerable.SetPower;

public class Intake implements Subsystem {
    public static final Intake INSTANCE = new Intake();
    private Intake() { }
    private MotorEx motor = new MotorEx("lift_motor");
    public Command start = new SetPower(motor, 0.5);
    public Command stop = new SetPower(motor, 0);
}