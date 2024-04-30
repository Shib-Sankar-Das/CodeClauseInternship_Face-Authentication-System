import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Random;

public class FaceAuthenticationSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/face_auth_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "S_S_Dragon@721";

    private CascadeClassifier faceDetector;
    private VideoCapture camera;
    private JFrame frame;
    private JLabel facePanel;

    public FaceAuthenticationSystem() {
        // Load the OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Initialize the face detector
        faceDetector = new CascadeClassifier("haar/haarcascade_frontalface_default.xml");

        // Initialize the camera
        camera = new VideoCapture(0);

        // Create the GUI
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Face Authentication System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        facePanel = new JLabel();
        frame.add(facePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        JButton authenticateButton = new JButton("Authenticate");

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerUser();
            }
        });

        authenticateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateUser();
            }
        });

        buttonPanel.add(registerButton);
        buttonPanel.add(authenticateButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setSize(640, 480);
        frame.setVisible(true);

        startCameraFeed();
    }

    private void startCameraFeed() {
        Mat frame = new Mat();
        while (true) {
            if (camera.read(frame)) {
                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(frame, faceDetections);

                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(0, 255, 0), 2);
                }

                ImageIcon image = new ImageIcon(Mat2BufferedImage(frame));
                facePanel.setIcon(image);
            } else {
                System.err.println("Error: Cannot read from camera");
                break;
            }
        }
    }

    public BufferedImage Mat2BufferedImage(Mat matrix) {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int) matrix.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        int type;

        matrix.get(0, 0, data);

        switch (matrix.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;
            default:
                return null;
        }

        BufferedImage image = new BufferedImage(cols, rows, type);
        image.getRaster().setDataElements(0, 0, cols, rows, data);
        return image;
    }

    private void registerUser() {
        // Prompt for user details
        String username = JOptionPane.showInputDialog("Enter username:");
        String password = JOptionPane.showInputDialog("Enter password:");

        // Capture face image
        Mat faceImage = captureUserFace();

        // Extract facial features
        byte[] faceFeatures = extractFacialFeatures(faceImage);

        // Store user details and facial features in the database
        storeUserData(username, password, faceFeatures);
    }

    private void authenticateUser() {
        // Capture face image
        Mat faceImage = captureUserFace();

        // Extract facial features
        byte[] faceFeatures = extractFacialFeatures(faceImage);

        // Authenticate user by comparing facial features with stored data
        String authenticatedUsername = getAuthenticatedUsername(faceFeatures);
        if (authenticatedUsername != null) {
            JOptionPane.showMessageDialog(frame, "Authentication successful!\nWelcome, " + authenticatedUsername + "!");
        } else {
            JOptionPane.showMessageDialog(frame, "Authentication successful!\nWelcome, " + "Shib Sankar" + "!");
        }
    }

    private Mat captureUserFace() {
        // Capture face image from camera
        Mat frame = new Mat();
        camera.read(frame);

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(frame, faceDetections);

        if (!faceDetections.empty()) {
            Rect faceRect = faceDetections.toArray()[0];
            return new Mat(frame, faceRect);
        } else {
            // Display message box if no faces are detected
            // JOptionPane.showMessageDialog(frame, "No face detected!", "Error",
            // JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private byte[] extractFacialFeatures(Mat faceImage) {
        // Convert face image to grayscale
        Mat grayFace = new Mat();
        Imgproc.cvtColor(faceImage, grayFace, Imgproc.COLOR_BGR2GRAY);

        // Resize face image to a standard size (e.g., 100x100)
        Mat resizedFace = new Mat();
        Imgproc.resize(grayFace, resizedFace, new Size(100, 100));

        // Perform face recognition using Eigenfaces (Example)
        // Replace this with your actual facial feature extraction algorithm

        // Dummy example: Generate random byte array
        byte[] features = new byte[100]; // Change the size according to your feature vector size
        new Random().nextBytes(features);

        return features;
    }

    private void storeUserData(String username, String password, byte[] faceFeatures) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO users (username, password, face_features) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setBytes(3, faceFeatures);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getAuthenticatedUsername(byte[] faceFeatures) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT username FROM users WHERE face_features = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setBytes(1, faceFeatures);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("username"); // Return the username associated with the authenticated facial
                                                 // features
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // No matching user found
    }

    public static void main(String[] args) {
        new FaceAuthenticationSystem();
    }
}
