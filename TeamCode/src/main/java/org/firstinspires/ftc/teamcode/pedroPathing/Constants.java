package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class Constants {

    // ================================================================
    // EDIT THIS SECTION ONLY — all robot-specific values live here
    //
    //   TRIONIX — FTC Team #36477
    //   Maintained by: Sarah
    // ================================================================

    // --- Robot ---
    // TODO: Weigh the robot WITH battery, in kg.
    private static final double ROBOT_MASS_KG = 10.0;

    // --- Drive motors (names must match RC configuration exactly) ---
    private static final String LEFT_FRONT_NAME  = "Front Left Motor";
    private static final String LEFT_REAR_NAME   = "Back Left Motor";
    private static final String RIGHT_FRONT_NAME = "Front Right Motor";
    private static final String RIGHT_REAR_NAME  = "Back Right Motor";

    // Verify with the Tuning drive test; flip any wheel that spins the wrong way.
    private static final DcMotorSimple.Direction LEFT_FRONT_DIR  = DcMotorSimple.Direction.REVERSE;
    private static final DcMotorSimple.Direction LEFT_REAR_DIR   = DcMotorSimple.Direction.REVERSE;
    private static final DcMotorSimple.Direction RIGHT_FRONT_DIR = DcMotorSimple.Direction.FORWARD;
    private static final DcMotorSimple.Direction RIGHT_REAR_DIR  = DcMotorSimple.Direction.FORWARD;

    private static final double MAX_POWER = 1.0;

    // Replace with results from Forward/Lateral Velocity tuners:
    private static final double X_VELOCITY = 81.34056;   // forward, in/s
    private static final double Y_VELOCITY = 65.43028;   // lateral, in/s

    // --- Pinpoint localizer (goBILDA Pinpoint + two 4-bar pods, 48mm wheels) ---
    // RC config: Pinpoint on an I2C bus, named as below.
    private static final String PINPOINT_NAME = "pinpoint";

    // TODO: Measure from robot's center of rotation, in inches:
    //   FORWARD_POD_Y: forward (X) pod's Y offset — left of center +, right −
    //   STRAFE_POD_X:  strafe (Y) pod's X offset — toward front +, toward back −
    private static final double FORWARD_POD_Y = 0;
    private static final double STRAFE_POD_X  = 0;

    // Verify with localization test: forward must increase X, strafe-left must
    // increase Y. Flip the offending direction if not.
    private static final GoBildaPinpointDriver.EncoderDirection FORWARD_POD_DIR =
            GoBildaPinpointDriver.EncoderDirection.REVERSED;
    private static final GoBildaPinpointDriver.EncoderDirection STRAFE_POD_DIR =
            GoBildaPinpointDriver.EncoderDirection.FORWARD;

    // --- Path constraints (tValue, timeout ms, velocity, translational) ---
    private static final double T_VALUE_CONSTRAINT       = 0.99;
    private static final double TIMEOUT_CONSTRAINT_MS    = 100;
    private static final double VELOCITY_CONSTRAINT      = 1;
    private static final double TRANSLATIONAL_CONSTRAINT = 1;

    // ================================================================
    // END OF EDIT SECTION — everything below just wires values together
    // ================================================================

    public static FollowerConstants followerConstants = new FollowerConstants()
            .mass(ROBOT_MASS_KG);

    public static MecanumConstants driveConstants = new MecanumConstants()
            .maxPower(MAX_POWER)
            .leftFrontMotorName(LEFT_FRONT_NAME)
            .leftRearMotorName(LEFT_REAR_NAME)
            .rightFrontMotorName(RIGHT_FRONT_NAME)
            .rightRearMotorName(RIGHT_REAR_NAME)
            .leftFrontMotorDirection(LEFT_FRONT_DIR)
            .leftRearMotorDirection(LEFT_REAR_DIR)
            .rightFrontMotorDirection(RIGHT_FRONT_DIR)
            .rightRearMotorDirection(RIGHT_REAR_DIR)
            .xVelocity(X_VELOCITY)
            .yVelocity(Y_VELOCITY);

    public static PinpointConstants localizerConstants = new PinpointConstants()
            .forwardPodY(FORWARD_POD_Y)
            .strafePodX(STRAFE_POD_X)
            .distanceUnit(DistanceUnit.INCH)
            .hardwareMapName(PINPOINT_NAME)
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            .forwardEncoderDirection(FORWARD_POD_DIR)
            .strafeEncoderDirection(STRAFE_POD_DIR);

    public static PathConstraints pathConstraints = new PathConstraints(
            T_VALUE_CONSTRAINT,
            TIMEOUT_CONSTRAINT_MS,
            VELOCITY_CONSTRAINT,
            TRANSLATIONAL_CONSTRAINT);

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .mecanumDrivetrain(driveConstants)
                .pinpointLocalizer(localizerConstants)
                .pathConstraints(pathConstraints)
                .build();
    }
}
