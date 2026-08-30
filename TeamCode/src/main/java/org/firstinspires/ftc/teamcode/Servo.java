package org.firstinspires.ftc.teamcode;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.subsystems.Subsystem;
import dev.nextftc.hardware.impl.ServoEx;
import dev.nextftc.hardware.positionable.SetPosition;

public class Servo implements Subsystem {
   public static final Servo INSTANCE = new Servo();
   private Servo() { }

   private ServoEx servo = new ServoEx("Test_servo");

   public Command open = new SetPosition(servo, 0.1).requires(this);
   public Command close = new SetPosition(servo, 0.2).requires(this);

}