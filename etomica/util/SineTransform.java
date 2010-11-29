package etomica.util;

/**
 * 3D Fourier transforms of a function, f, simplify to 1D sine transforms of the auxiliary function F(r) = r*f(r) 
 * when f is spherically symmetric.  
 * 
 * Discrete sine transforms can be carried out by a fast Fourier transform operating on the vector [0 F 0 reverse(-F)].  
 * FastFourierTransform.java is used as the FFT black box.
 *  
 * @author kate
 *
 */

public class SineTransform {
	
	public SineTransform() {
	}
	
	
	public double[] forward(double[] f, double del_r) {
	
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Make auxiliary vector, Fr = r*fr
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		int N = f.length;
		
		double[] Fr = new double[N]; 
	
		
		for (int n=0; n < N; n++) {
			
			Fr[n] = (n)*del_r*f[n];
			
		}
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Make auxiliary vector, Fr2 = [0,Fr[1:N-1],*0*,reverse(-Fr[1:N-1])], to evaluate DST with DFT
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		double[] Fr2 = new double[2*(N)];
		
		// Fr2[0] = 0; Fr2[N] = 0;

		for (int i = 1; i<N; i++) {
			Fr2[i] = Fr[i];           
			Fr2[N+i] = -Fr[N-i]; 
		}
		
		
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Perform DFT on auxiliary vector Fr2
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

		FastFourierTransform fourier = new FastFourierTransform();
		FastFourierTransform.BACKUP=false;
		
		double[] Fr2i = new double[2*(N)]; // no imaginary part
		fourier.setData(Fr2, Fr2i);
		fourier.transform();
		
		double[] Fk2 = new double[2*(N)];
		Fk2 = fourier.getImaginary();
	
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Extract the DST, Fk, from Fk2 = -1/N*[#,Fk,#,#]
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
		double del_k = Math.PI/((N)*del_r);
		
		double[] Fk = new double[N];
		
		for (int i = 0; i<N; i++) {
			
			 Fk[i] = -(N)*Fk2[i]; 
			
		}
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Compute fk (including zeroth mode) from Fk: 
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	
		double [] fk = new double [N];
		
	    for (int i = 1; i<N; i++) {
		    
		    // Special consideration of zeroth mode, fk(k=0):
	    	// Even if there were an Fk(k=0) we could not utilize it in the regular way
	    	fk[0] = fk[0] + 4.0*Math.PI*(Fr[i]*(i)*del_r)*del_r;
	    	
	    	
		    // Modes 1 through N-1: 
	    	
		     fk[i] = 4.0*Math.PI*(Fk[i]/((i)*del_k))*del_r; 
		    // Fk[0] corresponds to k=1.

		}
	    
		
		return fk;
	}
	
	public double[] reverse(double[] fk, double del_r) {
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Make auxiliary vector, Fk = k*fk, ignoring the k=0 mode  
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		int N = fk.length;
		
		double del_k = Math.PI/((N)*del_r);

	    double[] Fk = new double[N];
		
		for (int i=0;i<N;i++) {
		    
		    Fk[i] = fk[i]*(i)*del_k; 
			
			//  Fk[i] = fk[i+1]*(i+1)*Math.PI/r_max;
		    
		}
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Make auxiliary vector, Fk2 = [0,Fk[1:N-1],0,reverse(-Fk[1:N-1])], to evaluate DST with DFT
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		double[] Fk2 = new double[2*(N)];
	
		Fk2[0] = 0;
		Fk2[N] = 0; 
		
		for (int i=1; i<N; i++) {
			Fk2[i] = Fk[i]; 
			Fk2[2*N-i] = -Fk[i];    
		}
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Perform DFT on auxiliary vector Fk2
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		double[] Fk2i = new double[2*(N)]; // no imaginary part
		
		FastFourierTransform fourier = new FastFourierTransform();
		FastFourierTransform.BACKUP=false;
		fourier.setData(Fk2, Fk2i);
		fourier.transform();
		
		double[] Fr2 = new double[2*(N)];
		Fr2 = fourier.getImaginary();

		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Extract Fr from Fr2
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		double[] Fr = new double[N];
		
		for (int i=0; i<N; i++) {

			  Fr[i] = -(N)*Fr2[i]; 

		}

		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//% Compute fr from Fr=r*fr;
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

		double[] fr = new double[N];
			
		for (int i=1; i<N; i++) {

		    //Special consideration for zeroth mode
		    fr[0] = fr[0] + 1.0/(2.0*Math.PI*Math.PI)*(Fk[i]*(i)*del_k)*del_k;

		    fr[i] = 1.0/(2.0*Math.PI*Math.PI)*(Fr[i]/(i*del_r))*del_k;
			
		    
		}
		
		
		return fr;
	}

	

}
