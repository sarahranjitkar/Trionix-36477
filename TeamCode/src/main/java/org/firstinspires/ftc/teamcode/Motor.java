package org.firstinspires.ftc.teamcode;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.subsystems.Subsystem;
import dev.nextftc.hardware.impl.MotorEx;
import dev.nextftc.hardware.powerable.SetPower;

public class Motor implements Subsystem {
    public static final Motor INSTANCE= new Motor();
    private Motor(){}

    private MotorEx fishy= new MotorEx("fishyahh");

    public Command move = new SetPower(fishy, 1);
    public Command slow = new SetPower(fishy, 0.3);
    public Command stop = new SetPower(fishy, 0);
}
