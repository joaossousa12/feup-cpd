import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.min;


public class Matrix{
    private static final String PERFORMANCE_METRICS_FOLDER = "PerformanceMetricsJava/";

    private static void OnMult(int m_ar, int m_br, FileWriter fileWriter) throws IOException{
        int i,j,k;

        double temp;

        double pha[] = new double[m_ar * m_ar];
        double phb[] = new double[m_ar * m_ar];
        double phc[] = new double[m_ar * m_ar];

        for(i = 0; i < m_ar; i++)
            for(j = 0; j < m_ar; j++)
                pha[i * m_ar + j] = 1.0;

        for(i = 0; i < m_br; i++)
            for(j = 0; j < m_br; j++)
                phb[i * m_br + j] = i + 1;

        long time1 = System.nanoTime();

        for(i = 0; i < m_ar; i++){
            for(j = 0; j < m_br; j++){
                temp = 0;
                for(k = 0; k < m_ar; k++){
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }

        long time2 = System.nanoTime();

        double timeInSeconds = (time2 - time1) / 1_000_000_000.0;
        
        fileWriter.write("Time: " + String.format("%.3f", timeInSeconds) + " seconds\n");
        fileWriter.write("Result matrix: ");
        for (i = 0; i < 1; i++)
            for (j = 0; j < min(10, m_br); j++)
                fileWriter.write(phc[j] + " ");
        fileWriter.write("\n");
    }
    private static void OnMultLine(int m_ar, int m_br, FileWriter fileWriter) throws IOException{
        int i,j,k;

        double pha[] = new double[m_ar * m_ar];
        double phb[] = new double[m_ar * m_ar];
        double phc[] = new double[m_ar * m_ar];

        for(i = 0; i < m_ar; i++)
            for(j = 0; j < m_ar; j++)
                pha[i * m_ar + j] = 1.0;

        for(i = 0; i < m_br; i++)
            for(j = 0; j < m_br; j++)
                phb[i * m_br + j] = i + 1;

        long time1 = System.nanoTime();

        for(i=0; i < m_ar; i++)
		    for(k = 0; k < m_ar; k++)
			    for(j = 0; j < m_br; j++)
				    phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];

        long time2 = System.nanoTime();

        double timeInSeconds = (time2 - time1) / 1_000_000_000.0;
        
        fileWriter.write("Time: " + String.format("%.3f", timeInSeconds) + " seconds\n");
        fileWriter.write("Result matrix: ");
        for (i = 0; i < 1; i++)
            for (j = 0; j < min(10, m_br); j++)
                fileWriter.write(phc[j] + " ");
        fileWriter.write("\n");
    }
    public static void main(String[] args){
        try{
            FileWriter onMultFile1 = new FileWriter(PERFORMANCE_METRICS_FOLDER + "onmult.txt");
            FileWriter onMultLineFile1 = new FileWriter(PERFORMANCE_METRICS_FOLDER + "onmultLine.txt");

            int[] mult = {600, 1000, 1400, 1800, 2200, 2600, 3000};
            int[] multLine = {600, 1000, 1400, 1800, 2200, 2600, 3000};

            for (int i : mult) {
                System.out.println(i + "\n");
                onMultFile1.write(i + "x" + i + "\n\n");
                for (int j = 0; j < 3; j++) {
                    onMultFile1.write((j + 1) + ",");
                    OnMult(i, i, onMultFile1);
                    onMultFile1.write("\n");
                }
            }

            for (int i : multLine) {
                System.out.println(i + "\n");
                onMultLineFile1.write(i + "x" + i + "\n\n");
                for (int j = 0; j < 3; j++) {
                    onMultLineFile1.write((j + 1) + ",");
                    OnMultLine(i, i, onMultLineFile1);
                    onMultLineFile1.write("\n");
                }
            }

            onMultFile1.close();
            onMultLineFile1.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
