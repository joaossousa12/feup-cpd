#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <omp.h>
#include <cmath>

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



	#pragma omp parallel for
	for(int l=0; l < m_ar; l++)
		for(int k = 0; k < m_ar; k++)
			for(int z = 0; z < m_br; z++)
				phc[l * m_ar + z] += pha[l * m_ar + k] * phb[k * m_br + z];

	free(pha);
	free(phb);
	free(phc);


}

void OnMultLineParallel2(int m_ar, int m_br)
{	
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


	#pragma omp parallel 
	for(int l=0; l < m_ar; l++)
		for(int k = 0; k < m_ar; k++)
			#pragma omp for
			for(int z = 0; z < m_br; z++)
				phc[l * m_ar + z] += pha[l * m_ar + k] * phb[k * m_br + z];


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



	vector<int> mult = {600,1000,1400,1800,2200,2600,3000};
	vector<int> multLine = {600,1000,1400,1800,2200,2600,3000,4096,6144,8192,10240};
	vector<int> multBlock = {4096,6144,8192,10240};
	vector<int> multLineParallel = {600,1000,1400,1800,2200,2600,3000,4096,6144,8192,10240};
	vector<int> blockSize = {128,256,512};

	SYSTEMTIME Time1, Time2, Time3, Time4, Time5, Time6;
	double start, end;
	double start1, end1;
	double serialTime, parallelTime;

	double serialTimes[] = {
        0.105,
        0.540,
        1.682,
        3.426,
        6.309,
        10.604,
        16.506,
        42.260,
        141.868,
        331.963,
        650.601
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

	double speedup = 0;

	for(int i :  multLineParallel){
		onMultLineParallelOut <<endl<< i << "x" << i << endl << endl;
		onMultLineParallelOut2 <<endl<< i << "x" << i << endl << endl;
		double parallelTimeavg = 0;
		double parallelTimeavg1 = 0;
		for(int j = 0; j < 3; j++){
			onMultLineParallelOut << j + 1 << ",";
			onMultLineParallelOut2 << j + 1 << ",";
	
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;


			start = omp_get_wtime();
			OnMultLineParallel(i, i);
			end = omp_get_wtime();

			parallelTime = (double)(end - start);
	
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
	

			onMultLineParallelOut << "ParallelTime:" << parallelTime << ",L1 DCM:" << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << endl;
			
		    parallelTimeavg1 += parallelTime; 
                      

			ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 

			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

			start1 = omp_get_wtime();
			OnMultLineParallel2(i, i);
			end1 = omp_get_wtime();

			parallelTime = (double)(end1 - start1);

			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;

			onMultLineParallelOut2 << "ParallelTime:" << parallelTime << ",L1 DCM:" << values[0] << ",L2 DCM:" << values[1] << ",L2 DCA:" << values[2] << endl;
			
		    parallelTimeavg += parallelTime;
                      

			ret = PAPI_reset( EventSet );
			if ( ret != PAPI_OK ) std::cout << "FAIL reset" << endl; 
			

		}
		parallelTimeavg /= 3;
		parallelTimeavg1 /= 3;
		speedup = serialTimes[serialTimeIndex] / parallelTimeavg1;
		// efficiency for 8 cores
		onMultLineParallelOut << endl << endl << "MFLOPS:" << (2 * (pow(i,3))) / (parallelTimeavg1 * 1000000) 
		<< ",ParallelTimeAvg:" << parallelTimeavg1
		<< ",Speedup:" << speedup << ",Efficiency:" << speedup/8 << endl;
		speedup = serialTimes[serialTimeIndex] / parallelTimeavg;
		onMultLineParallelOut2 << endl << endl << "MFLOPS:" << (2 * (pow(i,3))) / (parallelTimeavg * 1000000) 
		<< ",ParallelTimeAvg:" << parallelTimeavg
		<< ",Speedup:" << speedup << ",Efficiency:" << speedup/8 << endl;
		serialTimeIndex++;
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
