package dpassos.interactor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class ScreenInteractor {
	
	private static final int SCREEN_UPDATE_WORKERS = 4;
	private static final boolean ROTATE = false;
	
	static String DEVICE_ID = "";
	static String DEVICE_PARAM = "";
	static String ADB_PATH = "/Users/admin/Library/Android/sdk/platform-tools/adb ";	
	static String ANDROID_FOLDER = "/sdcard/screen_capture/";
	static String COMPUTER_FOLDER = "/Users/admin/screen_capture/";
	static String FILE_NAME = ANDROID_FOLDER+"screen";
	static ExecutorService capturePool = Executors.newFixedThreadPool(SCREEN_UPDATE_WORKERS);
	static ExecutorService deletePool = Executors.newSingleThreadExecutor();
	static boolean terminate = false;
	static int proportion = 1;
	static int margin = 10;
	static Runtime runtime = Runtime.getRuntime();
	static Integer screenNumber = 1;
	static JLabel imageViewer;
	static JFrame screen;
	static JTextArea msg;
	static double RADIAN_DEGREES_TORATE = Math.toRadians(270);
	
	public static void main(String[] args) {
		
		configureDevices();
		
		File localFolder = new File(COMPUTER_FOLDER);
		localFolder.mkdirs();
		
		BorderLayout bl = new BorderLayout();
		screen = new JFrame("Screen Interactor");
		screen.setSize(320, 720);
		screen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		screen.setLayout(bl);
		screen.setLocationRelativeTo(null);
		screen.setAlwaysOnTop(true);
		
		msg = new JTextArea("Screen Interactor Device : " + DEVICE_ID);
		msg.setEditable(false);
		msg.requestFocus(false);
		msg.setMargin(new Insets(margin, margin, margin, margin));
		
		imageViewer = new JLabel();
		imageViewer.setBackground(Color.GRAY);
		
		imageViewer.setPreferredSize(new Dimension(screen.getSize().width-margin, screen.getSize().height-margin));
		imageViewer.setHorizontalAlignment(SwingConstants.CENTER);
		
		screen.add(msg, BorderLayout.NORTH);
		screen.add(imageViewer, BorderLayout.CENTER);
		screen.addWindowListener(new WindowListener() {			
			@Override
			public void windowOpened(WindowEvent e) {
			}
			@Override
			public void windowIconified(WindowEvent e) {
			}
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			@Override
			public void windowClosing(WindowEvent e) {
			}
			@Override
			public void windowClosed(WindowEvent e) {
				terminate = true;
			}
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		
		screen.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}			
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					sendKey(runtime, ""+e.getKeyChar());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		imageViewer.addMouseMotionListener(new MouseMotionListener() {
			boolean dragging, processing;
			int startX, endX, startY, endY;
			@Override
			public void mouseMoved(MouseEvent e) {
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				try {
					if(!dragging) {
						System.out.println("Starting");
						startX = e.getX();
						endX = e.getX();
						startY = e.getY();
						endY = e.getY();
						dragging = true;
					}else{
						endX = e.getX();
						endY = e.getY();
						if(Math.abs(startY - endY) > 20 || (Math.abs(startX - endX) > 20)){
							if(!processing) {
								processing = true;
							}else{
								return;
							}
							System.out.println("X"+e.getX()+" Y"+e.getY());
							String command = 
									ADB_PATH + "shell input swipe "+(proportion * endX)+ " "+ (proportion * endY)+" "+(proportion * startX)+ " "+ (proportion * startY);
							Process p = runtime.exec(command);
							System.out.println(command);
							p.waitFor();
							dragging = false;
							processing = false;
							mouseScreenUpdateRequest();
							screen.requestFocus();
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}	
			}
		});
		
		imageViewer.addMouseListener(new MouseListener() {			
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}			
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					System.out.println("X"+e.getX()+" Y"+e.getY());
					String command = ADB_PATH + "shell input tap "+(proportion * e.getX())+ " "+ (proportion * e.getY());
					Process p = runtime.exec(command);
					System.out.println(command);
					p.waitFor();
					mouseScreenUpdateRequest();
					screen.requestFocus();
				} catch (Exception e1) {
					e1.printStackTrace();
				}				
			}
		});
		
		try {
			createFolder(runtime);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		screen.setVisible(true);
		screen.requestFocus();
		
		while(!terminate) {
			updateScreen();
			delay(100);
		}
		capturePool.shutdown();
		deletePool.shutdown();
	}
	
	static void mouseScreenUpdateRequest() {
		synchronized (screenNumber) {
			screenNumber++;
			execute(imageViewer, runtime, screenNumber);			
		}
	}
	
	static void delay(int value){
		try {
			Thread.sleep(value);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	static void updateScreen() {
		synchronized (screenNumber) {
			screenNumber++;
			execute(imageViewer, runtime, screenNumber);			
		}
	}
	
	static void execute(JLabel l, Runtime runtime, int i) {
		capturePool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					tekeAndroidScreenShot(runtime, i);
					getLocalImage(l, runtime, l.getWidth(), i);
					delete(runtime, i);
				} catch (Exception e) {
					e.printStackTrace();
				}					
			}
		});
	}
	
	static void resetResolution(Runtime runtime) throws Exception {
		String command = ADB_PATH +" shell wm size reset";
		Process p = runtime.exec(command);
		p.waitFor();		
		command = ADB_PATH +" shell wm density reset";
		p = runtime.exec(command);		
		p.waitFor();
		
		command = ADB_PATH +" reboot";
		p = runtime.exec(command);		
		p.waitFor();		
	}
	
	static void changeResolution(Runtime runtime) throws Exception {
		String command = ADB_PATH +" shell wm size 1080x1920";
		Process p = runtime.exec(command);
		p.waitFor();		
		command = ADB_PATH +" shell wm density 441";
		p = runtime.exec(command);		
		p.waitFor();
		
		command = ADB_PATH +" reboot";
		p = runtime.exec(command);		
		p.waitFor();		
	}
	
	static void createFolder(Runtime runtime) throws Exception {
		String command = ADB_PATH +" shell mkdir "+ANDROID_FOLDER;
		Process p = runtime.exec(command);
		p.waitFor();
	}
	
	static void delete(Runtime runtime, int id) throws Exception {
		String command = ADB_PATH +" shell rm -f "+FILE_NAME+id+".png ";
		Process p = runtime.exec(command);
		p.waitFor();
	}
	
	static void getLocalImage(JLabel l, Runtime runtime, int width, int imageId) throws Exception{
		Image img = loadImageToComputer(runtime,width, imageId);		
		ImageIcon icon = new ImageIcon(img);
		l.setIcon(icon);
		screen.setSize(icon.getIconWidth() + margin, icon.getIconHeight() + msg.getHeight() + 3 * margin);
	}
	
	static void tekeAndroidScreenShot(Runtime runtime, int id) throws Exception {
		String command = ADB_PATH +" shell screencap "
				+FILE_NAME +id+".png ";
		Process p = runtime.exec(command);
		p.waitFor();
	}
	
	static void sendKey(Runtime runtime, String text) throws Exception {
		String command = ADB_PATH +" shell input text  '"+text+"'";
		Process p = runtime.exec(command);
		p.waitFor();
	}
	
	static Image loadImageToComputer(Runtime runtime, int width, int id) throws Exception {
		File file = new File(COMPUTER_FOLDER+"screen"+id+".png");
		file.createNewFile();

		String command = ADB_PATH+" pull "
				+FILE_NAME +id+".png "+COMPUTER_FOLDER;

		Process p = runtime.exec(command);
		p.waitFor();

		BufferedImage bufferedImage = ImageIO.read(file);
		
		if(ROTATE){
			bufferedImage = rotate(bufferedImage);
		}
		
		deletePool.execute(new Runnable() {
			@Override
			public void run() {
				file.delete();				
			}
		});
		
		proportion = bufferedImage.getWidth() / width;
		if(proportion == 0) proportion = 1;
		
		return bufferedImage.getScaledInstance((bufferedImage.getWidth() / proportion) - margin,
				(bufferedImage.getHeight() / proportion) - margin, java.awt.Image.SCALE_FAST);
				
	}
	
	static String listDevices(Runtime runtime) throws Exception {
		String command = ADB_PATH +" devices ";
		System.out.println(command);
		Process p = runtime.exec(command);
		p.waitFor();
		int byteLido;
		String str = "";
		while((byteLido = p.getInputStream().read()) != -1) {
			str+= (char) byteLido;
		}
		return str;
	}
	
	static void configureDevices() {
		try {
			String response = listDevices(runtime);
			String[] lines = response.split("\n");
			if(lines == null || lines.length <=1) {
				JOptionPane.showMessageDialog(null, "No devices try later");
				System.exit(0);
				return;
			}
			
			String options = ""; 
			int i = 0;
			for(String line : lines) {
				if(i > 0) {
					options += " "+ line;					
				}
				i++;
			}
			String[] optionArray = options.trim().split(" "); 
			int selected = JOptionPane.showOptionDialog(null, "Choose a device", "Devices", JOptionPane.DEFAULT_OPTION, 
					JOptionPane.INFORMATION_MESSAGE, 
					null, optionArray , "CANCEL");
			if(selected != -1) {
				String type = optionArray[selected].split("\t")[1];
				if("device".equalsIgnoreCase(type)) {
					DEVICE_PARAM = " -s ";	
				}else {
					DEVICE_PARAM = " -d ";
				}
				DEVICE_ID = optionArray[selected].split("\t")[0];	
				
				
				ADB_PATH += DEVICE_PARAM + DEVICE_ID + " "; 				
			}else {
				JOptionPane.showMessageDialog(null, "No device selected try later");
				System.exit(0);
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}
	
	static BufferedImage rotate(BufferedImage source) {
		BufferedImage output = new BufferedImage(source.getHeight(), source.getWidth(), source.getType());
        
		AffineTransform transform = new AffineTransform();
        transform.rotate(RADIAN_DEGREES_TORATE, source.getWidth()/2, source.getHeight()/2);
        double offset = (source.getWidth()-source.getHeight())/2;
        transform.translate(-offset,-offset);
		
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        op.filter(source, output);
        
        return output;
	}
}
