import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class VisualizerApp {
	
	static File file;
	static MainWindow window;
	static VisualizerWindow visualizer;
	static AudioInputStream stream;
	static Clip clip;
	static boolean isFileSelected = false;
	
	static int[] heights = new int[100];
	static double throughSong = 0;
	
	public static void main(String[] args) {
		window = new MainWindow();
		window.setVisible(true);
	}
	
	static void fileSelected() {
		isFileSelected = true;
		
		window.panel.file.setText(file.getName());
		window.panel.filePath.setText(file.getPath());
		window.panel.fileSize.setText(file.length() + " bytes");
		
		try {
			stream = AudioSystem.getAudioInputStream(file);
			clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, stream.getFormat()));
			clip.open(stream);
			
			DecimalFormat format = new DecimalFormat("00.00");
			window.panel.songLength.setText((int)(clip.getMicrosecondLength() / 6e7) + ":" + format.format((clip.getMicrosecondLength() / 1e6) % 60));
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	static void openVisualizer() {
		if(isFileSelected) {
			try {
				
				stream = AudioSystem.getAudioInputStream(file);
				clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, stream.getFormat()));
				clip.open(stream);
				
				stream = AudioSystem.getAudioInputStream(file);
				byte[] bytes = new byte[(int) stream.getFrameLength() * stream.getFormat().getFrameSize()];
				stream.read(bytes);
				
				int[][] data = new int[stream.getFormat().getChannels()][(int) stream.getFrameLength()];
				int sampleIndex = 0;

				for (int t = 0; t < bytes.length; sampleIndex++) {
					for (int channel = 0; channel < stream.getFormat().getChannels(); channel++) {
						int low = (int) bytes[t];
						t++;
						int high = (int) bytes[t];
						t++;
						data[channel][sampleIndex] = (high << 8) + (low & 0x00ff);
					}
				}
				
				visualizer = new VisualizerWindow();
				
				visualizer.addWindowListener(new WindowListener() {

					@Override
					public void windowActivated(WindowEvent arg0) {}
					@Override
					public void windowClosed(WindowEvent arg0) {}
					@Override
					public void windowClosing(WindowEvent arg0) {
						clip.stop();
						clip.close();
					}
					@Override
					public void windowDeactivated(WindowEvent arg0) {}
					@Override
					public void windowDeiconified(WindowEvent arg0) {}
					@Override
					public void windowIconified(WindowEvent arg0) {}
					@Override
					public void windowOpened(WindowEvent arg0) {}
					
				});
				
				visualizer.addWaves(20, 20, 100, 100);
				visualizer.addWaves(20, visualizer.getHeight() - 120, 100, 100);
				visualizer.addWaves(visualizer.getWidth() - 120, 20, 100, 100);
				visualizer.addWaves(visualizer.getWidth() - 120, visualizer.getHeight() - 120, 100, 100);
				visualizer.addWaves(120, 120, visualizer.getWidth() - 240, visualizer.getHeight() - 240);
				
				clip.start();
				
				long startTime = System.nanoTime();
				
				while (true) {
					long timeElapsed = System.nanoTime() - startTime;
					double framesPassed = timeElapsed * stream.getFormat().getSampleRate() / 1e9;
					for (int i = 0; i < heights.length; i++)
						heights[i] = data[0][(int) Math.max(0, framesPassed - heights.length / 2 + i)] / 60;
					throughSong = (double)timeElapsed / 1000 / clip.getMicrosecondLength();
					visualizer.repaint();
				}
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}
}

@SuppressWarnings("serial")
class VisualizerWindow extends JFrame {
	VisualizerWindow() {
		setTitle("Visualization");
		setVisible(true);
		setSize(1000, 1000);
		setLayout(null);
		ProgressBarPanel progressBar = new ProgressBarPanel();
		add(progressBar);
		progressBar.setBounds(0, 0, getWidth(), 20);
	}

	void addWaves(int x, int y, int w, int h) {
		VisualizerPanel panel = new VisualizerPanel();
		add(panel);
		panel.setBounds(x, y, w, h);
	}
}

@SuppressWarnings("serial")
class ProgressBarPanel extends JPanel {
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.setColor(Color.GREEN);
		g.fillRect(0, 0, (int)((double) getWidth() * VisualizerApp.throughSong), getHeight());
	}
}

@SuppressWarnings("serial")
class VisualizerPanel extends JPanel {
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.BLACK);

		double l = VisualizerApp.heights.length, w = getWidth(), h = getHeight();

		for (int i = 0; i < l - 1; i++)
			g.drawLine(
					(int) (w / l * (i + 0.5)),
					(int) (h / 2 + VisualizerApp.heights[i] * ((-4 * Math.pow(i - (l - 1) / 2, 2)) / Math.pow((l - 1), 2) + 1) * (h / 600)),
					(int) (w / l * (i + 1.5)),
					(int) (h / 2 + VisualizerApp.heights[i + 1] * ((-4 * Math.pow((i + 1) - (l - 1) / 2, 2)) / Math.pow((l - 1), 2) + 1) * (h / 600)));
	}
}

@SuppressWarnings("serial")
class MainWindow extends JFrame {
	
	MainPanel panel;
	
	MainWindow() {
		setTitle("Music Visualizer");
		setSize(600, 600);
		panel = new MainPanel();
		add(panel);
		panel.setBounds(getBounds());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.revalidate();
		panel.repaint();
	}
}

@SuppressWarnings("serial")
class MainPanel extends JPanel {
	
	JTextField info, file, filePathHeader, filePath, fileSizeHeader, fileSize, songLengthHeader, songLength;
	
	MainPanel() {
		setLayout(new BorderLayout());
		
		JFileChooser fileChoose = new JFileChooser("Downloads");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(".wav files", "wav");
		fileChoose.setFileFilter(filter);
		
		fileChoose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("ApproveSelection")) {
					VisualizerApp.file = fileChoose.getSelectedFile();
					VisualizerApp.fileSelected();
				}
				if(e.getActionCommand().equals("CancelSelection"))
					System.exit(0);
			}
		});
		
		JButton submit = new JButton("Run");
		
		submit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						VisualizerApp.openVisualizer();
					}
				}).start();
			}
		});
		
		JTextField title = new JTextField("Select a .wav file to visualize");
		title.setFont(new Font(title.getFont().getName(), title.getFont().getStyle(), title.getFont().getSize() + 10));
		title.setHorizontalAlignment(JTextField.CENTER);
		title.setEditable(false);
		title.setFocusable(false);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new GridLayout(0, 1));
		
		info = new JTextField("Selected file:");
		file = new JTextField();
		filePathHeader = new JTextField("Path:");
		filePath = new JTextField();
		fileSizeHeader = new JTextField("Size:");
		fileSize = new JTextField();
		songLengthHeader = new JTextField("Length:");
		songLength = new JTextField();
		
		info.setEditable(false);
		info.setFocusable(false);
		file.setEditable(false);
		file.setFocusable(false);
		filePathHeader.setEditable(false);
		filePathHeader.setFocusable(false);
		filePath.setEditable(false);
		filePath.setFocusable(false);
		fileSizeHeader.setEditable(false);
		fileSizeHeader.setFocusable(false);
		fileSize.setEditable(false);
		fileSize.setFocusable(false);
		songLengthHeader.setEditable(false);
		songLengthHeader.setFocusable(false);
		songLength.setEditable(false);
		songLength.setFocusable(false);
		
		info.setFont(info.getFont().deriveFont(Font.BOLD));
		filePathHeader.setFont(filePathHeader.getFont().deriveFont(Font.BOLD));
		fileSizeHeader.setFont(fileSizeHeader.getFont().deriveFont(Font.BOLD));
		songLengthHeader.setFont(songLengthHeader.getFont().deriveFont(Font.BOLD));
		
		add(title, BorderLayout.PAGE_START);
		add(centerPanel, BorderLayout.CENTER);
		add(submit, BorderLayout.PAGE_END);
		
		centerPanel.add(fileChoose, BorderLayout.PAGE_START);
		centerPanel.add(infoPanel, BorderLayout.PAGE_END);
		
		infoPanel.add(info);
		infoPanel.add(file);
		infoPanel.add(filePathHeader);
		infoPanel.add(filePath);
		infoPanel.add(fileSizeHeader);
		infoPanel.add(fileSize);
		infoPanel.add(songLengthHeader);
		infoPanel.add(songLength);
	}
}
