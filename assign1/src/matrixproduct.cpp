#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <omp.h>

using namespace std;

#define SYSTEMTIME clock_t

ofstream onMultOut("PerformanceMetrics/onmult.txt");
ofstream onMultLineOut("PerformanceMetrics/onmultLine.txt");
ofstream onMultBlockOut("PerformanceMetrics/onmultBlock.txt");
ofstream onMultLineParallelOut("PerformanceMetrics/onmultLineParallel.txt");
ofstream onMultLineParallelOut2("PerformanceMetrics/onmultLineParallel2.txt");

void OnMult(int m_ar, int m_br) 
{
	
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

		
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);



    Time1 = clock();

	for(i=0; i<m_ar; i++)
	{	for( j=0; j<m_br; j++)
		{	temp = 0;
			for( k=0; k<m_ar; k++)
			{	
				temp += pha[i*m_ar+k] * phb[k*m_br+j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


    Time2 = clock();
	onMultOut << "Time:" << fixed << setprecision(3) << (double)(Time2 - Time1) / CLOCKS_PER_SEC << " seconds,";

    free(pha);
    free(phb);
    free(phc);
	
	
}

// add code here for line x line matriz multiplication
void OnMultLine(int m_ar, int m_br)
{	
    SYSTEMTIME Time1, Time2;
    double *pha, *phb, *phc;
	int i, j, k;
	
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);

	Time1 = clock();

	for(i=0; i < m_ar; i++)
		for(k = 0; k < m_ar; k++)
			for(j = 0; j < m_br; j++)
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
		
	Time2 = clock();
	onMultLineOut << "Time:" << fixed << setprecision(3) << (double)(Time2 - Time1) / CLOCKS_PER_SEC << " seconds,";

	free(pha);
	free(phb);
	free(phc);
}

void OnMultLineParallel(int m_ar, int m_br)
{	
	SYSTEMTIME Time1, Time2;
	double *pha, *phb, *phc;
	int i, j;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);


	Time1 = clock();

	#pragma omp parallel for
	for(int l=0; l < m_ar; l++)
		for(int k = 0; k < m_ar; k++)
			for(int z = 0; z < m_br; z++)
				phc[l * m_ar + z] += pha[l * m_ar + k] * phb[k * m_br + z];

	Time2 = clock();

	onMultLineParallelOut << "Time:" << fixed << setprecision(3) << (double)(Time2 - Time1) / CLOCKS_PER_SEC << " seconds,";

	free(pha);
	free(phb);
	free(phc);


}

void OnMultLineParallel2(int m_ar, int m_br)
{	
	SYSTEMTIME Time1, Time2;
	double *pha, *phb, *phc;
	int i, j;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);


	Time1 = clock();

	#pragma omp parallel 
	for(int l=0; l < m_ar; l++)
		for(int k = 0; k < m_ar; k++)
			#pragma omp for
			for(int z = 0; z < m_br; z++)
				phc[l * m_ar + z] += pha[l * m_ar + k] * phb[k * m_br + z];

	Time2 = clock();

	onMultLineParallelOut2 << "Time:" << fixed << setprecision(3) << (double)(Time2 - Time1) / CLOCKS_PER_SEC << " seconds,";

	free(pha);
	free(phb);
	free(phc);


}
// add code here for block x block matriz multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize)
{
    SYSTEMTIME Time1, Time2;
    double *pha, *phb, *phc;
	int i, j, k, l, m, n;
	
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);

	Time1 = clock();

	for (i = 0; i < m_ar; i += bkSize)
        for (j = 0; j < m_br; j += bkSize)
            for (k = 0; k < m_ar; k += bkSize)
                for (l = i; l < min(i + bkSize, m_ar); l++) // rows pha current block
                    for (m = j; m < min(j + bkSize, m_br); m++) // columns phb current block
                        for (n = k; n < min(k + bkSize, m_ar); n++)
                            phc[l * m_ar + m] += pha[l * m_ar + n] * phb[n * m_br + m];

	Time2 = clock();
	onMultBlockOut << "Time:" << fixed << setprecision(3) << (double)(Time2 - Time1) / CLOCKS_PER_SEC << " seconds,";

	free(pha);
	free(phb);
	free(phc);
}



void handle_error (int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0) handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}


int main (int argc, char *argv[])
{	
	int EventSet = PAPI_NULL;
  	long long values[3];
  	int ret;
	

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
		if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L1_DCM );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L2_DCM);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L2_DCA);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCA" << endl;

	ret = PAPI_add_event(EventSet, PAPI_FP_INS);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_FP_INS" << endl;


	vector<int> mult = {600,1000,1400,1800,2200,2600,3000};
	vector<int> multLine = {600,1000,1400,1800,2200,2600,3000,4096,6144,8192,10240};
	vector<int> multBlock = {4096,6144,8192,10240};
	vector<int> multLineParallel = {600,1000,1400,1800,2200,2600,3000,4096,6144,8192,10240};
	vector<int> blockSize = {128,256,512};

	SYSTEMTIME Time1, Time2, Time3, Time4, Time5, Time6;
	double serialTime, parallelTime;

	double serialTimes[] = {
        0.102, 0112, 0.101,
        0.545, 0.601, 0.473,
        1.794, 1.688, 1.563,
        3.414, 3.431, 3.434,
        6.312, 6.314, 6.302,
        10.499, 10.512, 10.8,
        16.937, 16.108, 16.473,
        45.138, 41.449, 42.192,
        142.224, 141.005, 140.376,
        330.790, 332.683, 331.415,
        651.341, 648.978, 650.485
    };

	// Changed to 3 times per method because it is just a lot of time

	for(int i : mult){
		onMultOut << i << "x" << i << endl << endl;
		for(int j = 0; j < 3; j++){
			onMultOut << j + 1 << ",";
	
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
	
			OnMult(i, i);
	
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
	
			onMultOut << "L1 DCM: " << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << endl;
	
			ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 
		}
		onMultOut << endl;
	}

	for(int i : multLine){
		onMultLineOut << i << "x" << i << endl << endl;
		for(int j = 0; j < 3; j++){
			onMultLineOut << j + 1 << ",";
	
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

			Time1 = clock();
			OnMultLine(i, i);
			Time2 = clock();

			serialTime = (double)(Time2 - Time1) / CLOCKS_PER_SEC;

			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
	
			onMultLineOut << "L1 DCM: " << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << endl;
			
		    ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 
		}
		onMultLineOut << endl;
	}

	for(int i : multBlock){
		onMultBlockOut << i << "x" << i << endl << endl;
		for(int k : blockSize){
			onMultBlockOut << k << " block" << endl << endl;
			for(int j = 0; j < 3; j++){
				
				onMultBlockOut << j + 1 << ",";

				ret = PAPI_start(EventSet);
				if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

				OnMultBlock(i, i, k);

				ret = PAPI_stop(EventSet, values);
				if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
				
				onMultBlockOut << "L1 DCM: " << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << endl;
				
				ret = PAPI_reset( EventSet );
				if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 
			}
			onMultBlockOut << endl;
		}
		onMultBlockOut << endl;
	}
	int serialTimeIndex = 0;

	for(int i :  multLineParallel){
		onMultLineParallelOut << i << "x" << i << endl << endl;
		onMultLineParallelOut2 << i << "x" << i << endl << endl;
		for(int j = 0; j < 3; j++){
			onMultLineParallelOut << j + 1 << ",";
			onMultLineParallelOut2 << j + 1 << ",";
	
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;


			Time3 = clock();
			OnMultLineParallel(i, i);
			Time4 = clock();

			parallelTime = (double)(Time4 - Time3) / CLOCKS_PER_SEC;
			double speedup = serialTimes[serialTimeIndex] / parallelTime;
	
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
	
			onMultLineParallelOut << "L1 DCM: " << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << ",FLOPS:" << values[3] << endl;
			onMultLineParallelOut << "MFLOPS: " << values[3] / (parallelTime * 1000000) << endl;
			onMultLineParallelOut << "MFLOPS2: " << ((2*i)^3) / (parallelTime * 1000000) << endl;
			
		    
			onMultLineParallelOut << "Serial Time: " << serialTimes[serialTimeIndex] << " seconds, "
                      << "Parallel Time: " << parallelTime << " seconds, "
                      << "Speedup: " << speedup << endl;

			ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 

			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

			Time5 = clock();
			OnMultLineParallel2(i, i);
			Time6 = clock();

			parallelTime = (double)(Time6 - Time5) / CLOCKS_PER_SEC;
			speedup = serialTimes[serialTimeIndex] / parallelTime;

			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;

			onMultLineParallelOut2 << "L1 DCM: " << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << ",FLOPS:" << values[3] << endl;
			onMultLineParallelOut2 << "MFLOPS: " << values[3] / (parallelTime * 1000000) << endl;
			onMultLineParallelOut2 << "MFLOPS2: " << ((2*i)^3) / (parallelTime * 1000000) << endl;
			
		    
			onMultLineParallelOut2 << "Serial Time: " << serialTimes[serialTimeIndex] << " seconds, "
                      << "Parallel Time: " << parallelTime << " seconds, "
                      << "Speedup: " << speedup << endl;

			ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 

			serialTimeIndex++;
			

		}
		onMultLineParallelOut << endl;
		onMultLineParallelOut2 << endl;
	}

	ret = PAPI_remove_event( EventSet, PAPI_L1_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCA);
	if(ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

	onMultBlockOut.close();
	onMultOut.close();
	onMultLineOut.close();
	onMultLineParallelOut.close();
	onMultLineParallelOut2.close();
}
