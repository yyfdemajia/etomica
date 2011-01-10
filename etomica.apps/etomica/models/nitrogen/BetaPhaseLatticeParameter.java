package etomica.models.nitrogen;

/**
 * The parameters that are determined through conjugate gradient method to give the lowest lattice energy
 *  for beta-N2 phase structure
 * 
 * @author taitan
 *
 */
public class BetaPhaseLatticeParameter {
	public BetaPhaseLatticeParameter(){
		
	}
	
	public double[][] getParameter(double density){
		double[][] parameters;
		
		// these parameters are for nA=1024
		if(density==0.0250){
			parameters = new double[][]{
				{ 3.943623675439537E-5, -7.774687777255597E-5, -1.253954091950439E-6, -5.821076381066166E-4,  -1.1426375628492568E-4},
			    {-3.413464215138422E-5,  1.576735518174415E-4, -6.290086612653883E-6,  4.3312591345254193E-4, -1.6695297891638808E-4},
			    { 2.308775438977747E-6, -7.003860375718278E-5,  3.6468107523589976E-6,-2.5064580018316097E-4, -3.260837904268126E-4},
			    { 2.0040370120364808E-5, 1.3522603075083E-4,    3.979941964634359E-6,  5.208391332508404E-4,  -1.197854022044697E-4}
			};
		}else if(density==0.0240){
			parameters = new double[][]{
				{0.0041455759544500636, -9.973125892194278E-4, 0.0010654667332234066, -6.112646497823689E-4, -0.006091543797628076}, 
				{-0.004860191100274781, -0.0011185047819778846, 0.0010598981530198438, 4.553686694383018E-4, -0.006154751297776442}, 
				{-0.004822876003367027, -9.896044425178226E-4, 0.0010722741750401753, -2.699940955128142E-4, -0.006287800037644388}, 
				{0.00412436795091807, -0.0011411936940946507, 0.001074384513588608, 5.421605014910879E-4, -0.0061044675703691605}
			
			};
		} else if (density==0.0238){
			parameters = new double[][]{
				{0.006281296090595155, 0.0018636960324207065, 2.2110240804828693E-4, -0.0014036809054544244, -0.008449971754047568}, 
				{-0.004714745613562198, 0.0012650641596351473, 2.1536173401871355E-4, 0.0012467939175371222, -0.008514524358720084}, 
				{-0.004677708017512473, 0.001871373475529715, 2.275760062626014E-4, -0.0010607862675041499, -0.00864766987910912}, 
				{0.006259751637666007, 0.0012419882772394947, 2.299559332681835E-4, 0.0013340679895082646, -0.008464163570206894}
			
			};
			
		} else if (density==0.0236){
			parameters = new double[][]{
				{0.00587985895541576, 3.318719115741678E-4, 2.590043720731268E-4, -0.0024202818210948412, -0.011108812803056978}, 
				{-0.006840799353167392, -5.522148544040112E-4, 2.5340620457769037E-4, 0.0022629703361688575, -0.01117312661414254}, 
				{-0.0068037096583788426, 3.3965397522564555E-4, 2.6566579337048335E-4, -0.002077526073115998, -0.011306619503413384}, 
				{0.0058585116614836355, -5.7505802693964E-4, 2.677759602717458E-4, 0.0023510309480724322, -0.011122477252704037}
				
			};
			
		} else if (density==0.0234){
			parameters = new double[][]{
				{0.0075027058787762516, 0.0033989513550305036, 4.8687083861097063E-4, -0.0036299483934711984, -0.01392707117422452}, 
				{-0.006498710169229157, 0.00221226100907468, 4.811043882164454E-4, 0.003471764164319288, -0.013991813503532121}, 
				{-0.0064612608166665095, 0.0034066524637804117, 4.934453566205766E-4, -0.0032860156263545826, -0.014124884852503673}, 
				{0.007481336660995784, 0.002189447294110959, 4.956782260127964E-4, 0.0035610933667526786, -0.013940641137692313}
		
			};
			
		} else if (density==0.0232){
			parameters = new double[][]{
				{0.007559425210441226, 0.002144669727082795, 0.0010366834714885741, -0.004595884623994312, -0.016877284058530576}, 
				{-0.007407790842487786, 5.798327761478291E-4, 0.0010309478744233874, 0.004438994439937659, -0.01694125523540437}, 
				{-0.0073708455901976225, 0.0021522528745652747, 0.0010431945012894682, -0.0042530444501846995, -0.017074456971173102}, 
				{0.007538105657604441, 5.570274852839988E-4, 0.0010453664330827006, 0.004528222381208168, -0.016891046387864164}
			
			};		
			
		} else if (density==0.0230){
			parameters = new double[][]{
				{0.008643467781612603, 0.0026271868875943002, 0.0019564392850298443, -0.005368542841198517, -0.01963128167700018}, 
				{-0.007220434499475046, 7.968809489413192E-4, 0.0019507333095460664, 0.0052101555943571045, -0.019695510442517328}, 
				{-0.00718296472671309, 0.002634905247602907, 0.001963151739179857, -0.005024039971913238, -0.019828787213698948}, 
				{0.008622065304767177, 7.740172242212951E-4, 0.0019653547372104462, 0.005300512113324435, -0.019643975672095665}
		
			};	
			
		} else {
			throw new RuntimeException("<BetaPhaseLatticeParameter> Sorry I do not have the parameters you want!! " +
										"\n                         You have to do your own minimization procedure!!! HA! HA!");
		}
		
		
		return parameters;
	}
	
}
