/*
 * Crea una ventana redimensionable en la que aparecen dos columnas
 * que miden el volumen m'aximo registrado en dos canales de audio, a
 * 16 bits por muestra y usando el endian de la m'aquina.
 *
 * Los datos son le'idos de la entrada est'andar.
 *
 * gse. 2006, 2009.
 */

/* Evento de manipulaci'on de excepciones de entrada/salida. */
import java.io.IOException;

/* Un JComponent implementa las funciones m'as b'asicas de cualquier
 * objeto de la biblioteca Swing. En este caso se utiliza para pintar
 * el medidor de volumen. */
import javax.swing.JComponent;

/* Un Frame es una ventana con t'itulo y marco. */
import javax.swing.JFrame;

/* Almacena el contexto gr'afico. */
import java.awt.Graphics;

/* Define un color. */
import java.awt.Color;

public class VolMeter
    extends JComponent {

    /* Muestras pintadas (m'aximo de cada canal). */
    int leftMax, rightMax;
    
    /* Para mantener un hisorico temporal del volumen en cada canal. */
    int leftSand = 0, rightSand;
    double elasticity = 0.1;

    /* Anchura y altura por defecto de la ventana. */
    static final int WINDOW_WIDTH = 100;
    static final int WINDOW_HEIGHT = 500;

    /* Tasa de muestreo por cada canal. Si la tasa usada difiere de
     * esta, el c'alculo "Refresing time" no ser'a exacto. */
    static final double SAMPLING_RATE = 44100.0;

    /* Tama~no por defecto del buffer de audio, medido en bytes. Si
     * usamos 44100 muestras por segundo, 2 canales y 2 bytes/muestra,
     * tardar'iamos BUF_SIZE/4/44100 segundos en rellenarlo (y
     * en pintar las columnas del medidor de vol'umen). */
    static final int BUF_SIZE = 22050/*11025*/;
    
    /* Tama~no del buffer de audio. */
    int buf_size;

    /* El buffer de audio. */
    byte[] audio_buf;

    /* Tasa de muestreo (de cada canal). */
    int sampling_rate;

    /* Crea la ventana e inicializa las variables miembro. Finalmente
     * lanza el resto de la aplicaci'on que se encuentra almacenada en
     * el m'etodo "run()". */
    public VolMeter(int buf_size, double sampling_rate) {
	JFrame frame = new JFrame("VolMeter");
	frame.getContentPane().add(this);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
	this.buf_size = buf_size;
	audio_buf = new byte[buf_size];
	System.err.println("Buffer size: " + buf_size + " bytes");
	System.err.println("Sampling rate: " + sampling_rate + " Hz");
	System.err.println("Refresing time: " + (double)buf_size/4/SAMPLING_RATE + " seconds (" + 1.0/((double)buf_size/4/SAMPLING_RATE) + " frames per second)");
	run();
    }

    /* Lee muestras desde la entrada est'andar y las almacena en un
     * buffer. A continuaci'on se calcula el m'aximo del valor absoluto
     * de cada muestra, por cada canal. */
    public void run() {
	try {
	    for(;;) {
		/* Cargamos el buffer de datos. */
		int bytes_read = 0;
		while(bytes_read < buf_size) {
		    bytes_read += System.in.read(audio_buf, bytes_read, buf_size-bytes_read);
		}
		/*
		 * C'odigo alternativo para la carga del buffer de
		 * datos. Ser'ia v'alido siempre y cuando no se usen
		 * grandes buffers porque debido al tama~no m'aximo de
		 * los pipes, no es nunca superior a 22048.
		 */
		//int bytes_read = System.in.read(audio_buf);

		/* Para depurar ... */
		//System.err.println(bytes_read);

		/* Por si quisi'eramos continuar el pipe. */
		//System.out.write(audio_buf, 0, bytes_read);

		/* Calulamos el m'aximo de cada canal. */
		leftMax = rightMax = 0;
		for(int i=0; i</*buf_size*/bytes_read/4; i+=4) {
		    int left = (int)(audio_buf[i*2+1])*256 + (int)(audio_buf[i*2]);
		    int right = (int)(audio_buf[i*2+3])*256 + (int)(audio_buf[i*2+2]);
		    int absLeft = Math.abs(left);
		    int absRight = Math.abs(right);
		    if(leftMax<absLeft) leftMax = absLeft;
		    if(rightMax<absRight) rightMax = absRight;
		    if(leftSand<leftMax) leftSand = leftMax;
		    if(rightSand<rightMax) rightSand = rightMax;
		}

		/* Un "recordatorio" cercano del m'aximo volumen. */
		leftSand = (int)((1-elasticity)*leftSand + elasticity*leftMax);
		rightSand = (int)((1-elasticity)*rightSand + elasticity*rightMax);
		/* Pintamos el componente. */
		repaint();
	    }
	    
	} catch (IOException e) {
	    System.out.println("Error en el pipe");
	}
    }
    
    /* Pinta las gr'aficas. */
    public void paintComponent(Graphics g) {
	Color color;

	/* Canal izquierdo. */ {
	    color = Color.red;
	    g.setColor(color);
	    int l = (int)((double)(leftMax)/32768.0*getHeight());
	    if(leftMax >= 32000) {
		color = Color.black;
		g.setColor(color);
	    }
	    g.fillRect(0, getHeight()-l, getWidth()/2, l);

	    int ls = (int)((double)(leftSand)/32768.0*getHeight());
	    g.drawLine(0, getHeight()-ls, getWidth()/2, getHeight()-ls);
	}

	/* Canal derecho. */ {
	    color = Color.blue;
	    g.setColor(color);
	    int r = (int)((double)(rightMax)/32768.0*getHeight());
	    if(rightMax >= 32000) {
		color = Color.black;
		g.setColor(color);
	    }
	    g.fillRect(getWidth()/2, getHeight()-r, getWidth(), r);
	    
	    int rs = (int)((double)(rightSand)/32768.0*getHeight());
	    g.drawLine(getWidth()/2, getHeight()-rs, getWidth(), getHeight()-rs);
	}
    }
    
    public static void main(String args[]) throws Exception {
	int buf_size = BUF_SIZE;
	double sampling_rate = SAMPLING_RATE;
	try {
	    if(args.length >=1) {
		buf_size = Integer.parseInt(args[0]);
		sampling_rate = Integer.parseInt(args[1]);
	    }
	    new VolMeter(buf_size, sampling_rate);
	} catch (NumberFormatException e) {
	    System.out.println("Error parsing " + args[1]);
	}
    }
}
