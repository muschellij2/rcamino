package simulation.dynamics;

import numerics.MTRandom;
import simulation.DiffusionSimulation;
import simulation.SimulationParams;
import simulation.dynamics.StepGeneratorFactory.StepType;
import simulation.geometry.elements.Cylinder;
import simulation.geometry.substrates.StickyCylinderSubstrate;
import simulation.geometry.substrates.Substrate;
import tools.CL_Initializer;

/**
 * generates steps such that all beginning and end points
 * are on the surface of a cylinder (assumes that initial 
 * position is on the cylinder, if not oddness will ensue)
 *
 * @author matt (m.hall@cs.ucl.ac.uk)
 *
 */
public class CylindricalSurfaceStepGenerator implements StepGenerator {

    /** dimensionality of space */
    private final int D= DiffusionSimulation.D;
    
    /**
     * arclength of step. Note that is an arc length on the
     * surface of the cylinder. the step vectors themselves
     * will be the euclidean displacements from starting 
     * point to end point. i.e. they short-cut through the
     * cylinder and shouldn't be thought of as following
     * the cylindrical geodesic between the points.
     */
    private final double len;
    
    /**
     * radius of cylinder on which we are generating steps
     */
    private Cylinder cylinder;
    
    /**
     * substrate that we're stepping in
     */
    private final Substrate substrate;
    
    /**
     * random number generator
     */
    private final MTRandom stepTwister = new MTRandom(CL_Initializer.seed+1273);
    
    /**
     * test constructor. takes only step length
     * 
     * @param len arclength of step on surface
     */
    public CylindricalSurfaceStepGenerator(double len){
        
        this.len= len;
        
        this.substrate= null;
    }
    
    
    public CylindricalSurfaceStepGenerator(SimulationParams simParams){
        
        this.len=simParams.getStepParams()[1];
        
        this.substrate=null;
    }
    
    public CylindricalSurfaceStepGenerator(SimulationParams simParams, StickyCylinderSubstrate substrate){
        
        this.len=simParams.getStepParams()[1];
        
        this.substrate=substrate;
        
        this.cylinder= substrate.getCylinders()[0];
    }
    /** 
     * constructor that takes step length directly
     */
    public CylindricalSurfaceStepGenerator(Substrate substrate){
        
        this.len=SimulationParams.sim_surfaceDiffusivity;
        
        this.substrate= substrate;
    }
    
    /**
     * @see simulation.dynamics.StepGenerator#getBorder()
     */
    public double getBorder() {
        return len;
    }

    /**
     * Generates steps confined to the surface of the given cylinder.
     * 
     * Steps are generated by mapping the cylinder into the plane of
     * rTheta-z, which preserves distance in both directions. Steps
     * are randomly oriented in this unrolled plane.
     * 
     * Steps in the plane have two components: a linear z-component and
     * an angular theta component. Since radius is constant, the angle
     * increment dTheta is given by the arc length of the step component
     * on the circle. The euclidean displacements depend on the position
     * theta around the circle. These are given by
     * 
     * dx = p cos(theta) - q sin(theta)
     * dy = p sin(theta) + q cos(theta)
     * 
     * where p = p(dTheta) = r(cos(dTheta)-1) and
     *       q = q(dTheta) = r(sin(dTheta))
     *       
     *       with dTheta = l* /r the angular length of the step with arclength l*
     * 
     * note that no check is made for the fact that the walker actually 
     * IS located on the surface in question. this is assumed to be
     * the case and must be checked elsewhere.
     * 
     * @param walker the walker on the surface 
     * @return step on surface in euclidean coords
     */
    public double[] getStep(Walker walker) {
        
        // angle in upwrapped cylinder (rTheta-z) plane 
        double psi= stepTwister.nextDouble()*2.0*Math.PI;
        
        // axial part of step
        double dz= this.len*Math.cos(psi);
        
        // angular part of step
        // arc length of step on circle
        double lStar= this.len*Math.sin(psi);
        
        // change in angle on circle
        double l= lStar/cylinder.getRadius();
        
        // orientation
        double sgn=1.0;
        
        // flip orientation with prob 0.5
        if(stepTwister.nextBoolean()){
            sgn=-1.0;
        }
        
        // set angle increment
        double dTheta=sgn*l;
        
        // set increment factors
        double p= cylinder.getRadius()*(Math.cos(dTheta)-1);
        double q= cylinder.getRadius()*Math.sin(dTheta);
        
        // get substrate coords for walker
        final double[] pos= new double[D];
        walker.substrate.getSubstrateCoords(walker.r, new double[]{0.0, 0.0, 0.0}, pos);

        // get surface coords (assume on surface
        final double[] surfCoords= new double[D];
        getSurfaceCoords(pos, surfCoords, cylinder);
        
        // angular factors from position on cylinder
        double cosTh= Math.cos(surfCoords[1]);
        double sinTh= Math.sin(surfCoords[1]);
        
        // construct step
        return new double[]{p*cosTh - q*sinTh, 
                            p*sinTh + q*cosTh,
                            dz};
    }

    
    /** 
     * converts euclidean substrate coords into cylindrical
     * coords using the current cylinder as an origin.
     * 
     * note that no check is made for the fact that the walker
     * is genuinely on the surface. this is just assumed to
     * be the case.
     */
    private final void getSurfaceCoords(double[] subsCoords, double[] cylPos, Cylinder cylinder){
        
        double[] P= cylinder.getPosition();
        
        double[] euclideanCylCoords= new double[D];
        
        for(int i=0; i<D; i++){
            euclideanCylCoords[i]=subsCoords[i]-P[i];
        }
                
        cylPos[0]= Math.sqrt(euclideanCylCoords[0]*euclideanCylCoords[0] + euclideanCylCoords[1]*euclideanCylCoords[1]);
        cylPos[1]= Math.atan2(euclideanCylCoords[1], euclideanCylCoords[0]);
        cylPos[2]= euclideanCylCoords[2];

    }
    
    /**
     * set the current cylinder
     */
    public void setCylinder(Cylinder cylinder){
        this.cylinder= cylinder;
    }
    
    /**
     * @see simulation.dynamics.StepGenerator#getType()
     * 
     * @return cylindrical surface step generator type
     */
    public StepType getType() {
        return StepGeneratorFactory.StepType.CYLINDRICAL;
    }
    
    /**
     * @return finite size for a walker based on step length
     */
     public final double getWalkerRadius(){
    	 return len/LengthStepRatio;
     }
}