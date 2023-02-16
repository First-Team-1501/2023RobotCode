package frc.robot.Arm;

import java.util.ArrayList;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;

public class ArmCommand extends CommandBase
{
    private Arm arm;
    private ArmPose pose;

    private ArmCommand(Arm s_Arm, ArmPose pose)
    {
       
        this.arm = s_Arm;
        this.pose = pose;
        addRequirements(s_Arm);
    }

    @Override
    public void initialize()    
    {
       arm.adoptPose(pose);
    }

    @Override
    public void execute()
    {
       
    }

    @Override
    public void end(boolean inturrupted)
    {
        if(! inturrupted)
        {
        System.out.println("posed "+inturrupted);
        }
    }

    @Override
    public boolean isFinished()
    {
        
        return arm.isAtPose(pose);
    }


    public static Command PlotPath( NamedPose dest, Arm arm)
    {
        // a decent proxy for whether a gamepiece is held, I guess
        boolean hasPiece = arm.getClaw();
        
        System.out.println("Plotting path");
        ArmPose current = arm.getCurrentPose();
        ArmPose to = new PoseList().getArmPose(dest);

        ArrayList<Command> sequence = new ArrayList<Command>();
       
        if(arm.getExtender() && !to.getExtender())
        {
            sequence.add(new InstantCommand(()->arm.setExtender(false)));
            sequence.add(new WaitCommand(.5));
        }


        if(hasPiece)
        {
            sequence.addAll(AdjustForHeldPiece(to, arm));
        }

        sequence.add(new ArmCommand(arm,to));

        if(to.getExtender())
        {
            sequence.add(new InstantCommand(()->arm.setExtender(true)));
        }

        Command[] toReturn = new Command[sequence.size()];
        for(int i=0; i<sequence.size();i++ )
        {
            toReturn[i]=sequence.get(i);
        }
        
        
        Command c = Commands.sequence(toReturn);
        CommandScheduler.getInstance().schedule( c);
        return Commands.sequence(c);
    }

    private static ArrayList<Command> AdjustForHeldPiece(ArmPose to, Arm arm)
    {

        // pull the wrist up if J2 is passing 0.
        ArrayList<Command> sequence = new ArrayList<Command>();
        double crossTolerance = 5;
        ArmPose current = arm.getCurrentPose();
        
        System.out.println("Thinking of adding wristup...");
        
        
        if(isJ2OnSameSideOfTarget(current.getJ2(), to.getJ2(), crossTolerance))
        {
           
            
            // We want the waypoint to be BEFORE J2 passes zero, so crossTolerance will be set to the opposite of our goal.
            crossTolerance*= -Math.signum(to.getJ2());
            
            // AFAIK, we don't really care what J1 is doing. We want the wrist up, though.
            ArmPose p = new ArmPose((double)to.getJ1(), crossTolerance, 80.0, false);
            
             System.out.println("Added wristup: "+p);
            
            sequence.add(new ArmCommand(arm, p));
        }

        return sequence;
    }
    
    
    private static bool isJ2OnSameSideOfTarget(double J2, double ref, double clearance)
    {
        // What is the goal?
        // we want to know whether J2 on the same side as ref, and at least negTol away from zero.
      
        if( Math.signum(J2) != Math.signum(ref))
        {
            return false;
        }
        
        
        if(Math.Abs(J2) <= clearance)
        {
            return false;
        }
        
        return true;
    
    }


}
