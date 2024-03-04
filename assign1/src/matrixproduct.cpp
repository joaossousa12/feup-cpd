#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <time.h>
#include <cstdlib>
#include <papi.h>

using namespace std;

#define SYSTEMTIME clock_t

ofstream onMultOut("PerformanceMetrics/onmult.txt");
ofstream onMultLineOut("PerformanceMetrics/onmultLine.txt");
ofstream onMultBlockOut("PerformanceMetrics/onmultBlock.txt");
 
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
	vector<int> blockSize = {128,256,512};

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
	
			OnMultLine(i, i);
	
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
}
