import java.util.Scanner;

public class matrimult {

    public static double OnMult(int m_ar, int m_br) {

        long time1, time2;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];

        for (int a = 0; a < m_br; a++) {
            for (int b = 0; b < m_br; b++) {
                pha[a * m_br + b] = 1;
                phb[a * m_br + b] = a + 1;
                phc[a * m_br + b] = 0;
            }
        }
        time1 = System.currentTimeMillis();

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                double temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }

        time2 = System.currentTimeMillis();


        return (time2 - time1) / 1000.0;

    }

    public static double OnMultLine(int m_ar, int m_br){

        long startTime, endTime;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];

        for (int a = 0; a < m_br; a++) {
            for (int b = 0; b < m_br; b++) {
                pha[a * m_br + b] = 1;
                phb[a * m_br + b] = a + 1;
                phc[a * m_br + b] = 0;
            }
        }

        startTime = System.currentTimeMillis();

        for (int i = 0; i < m_ar; i++) {
            for (int k = 0; k < m_br; k++) {
                for (int j = 0; j < m_ar; j++) {
                    phc[i*m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                }
            }
        }

        endTime = System.currentTimeMillis();

        return (endTime - startTime) / 1000.0;
    }

     public static double OnMultBlock(int m_ar, int m_br, int bkSize){
        long startTime, endTime;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_br *  m_br];
        double[] phc = new double[m_ar * m_br];

        for (int a = 0; a < m_br; a++) {
            for (int b = 0; b < m_br; b++) {
                pha[a * m_br + b] = 1;
                phb[a * m_br + b] = a + 1;
                phc[a * m_br + b] = 0;
            }
        }

        startTime = System.currentTimeMillis();
        int i, ii, j, jj, k, kk;

        for(ii=0; ii<m_ar; ii+=bkSize) {
            for( kk=0; kk<m_ar; kk+=bkSize){
                for( jj=0; jj<m_br; jj+=bkSize) {
                    for (i = ii ; i < ii + bkSize ; i++) {
                        for (k = kk ; k < kk + bkSize ; k++) {
                            for (j = jj ; j < jj + bkSize ; j++) {
                                phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j];
                            }
                        }
                    }
                }
            }
        
     }

     endTime = System.currentTimeMillis();

     return (endTime - startTime) / 1000.0;

     }


    public static void main(String[] args) {

        if (args.length != 2) return;
        
        int dim = Integer.parseInt(args[0]);
        int bkSize = Integer.parseInt(args[1]);

        double total_time = 0;

        for(int i = 0; i < 10; i++)
            total_time += OnMult(dim, dim);
        total_time /= 10;
        System.out.printf("AVG Time Normal: %.3f seconds%n%n", total_time);

        for(int i = 0; i < 10; i++)
            total_time += OnMultLine(dim, dim);
        total_time /= 10;
        System.out.printf("AVG Time Line: %.3f seconds%n%n", total_time);

        for(int i = 0; i < 10; i++)
            total_time += OnMultBlock(dim, dim, bkSize);
        total_time /= 10;
        System.out.printf("AVG Time Block: %.3f seconds%n%n", total_time);

    }
}

