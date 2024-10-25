import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class Simple3DExample extends Application {

    @Override
    public void start(Stage stage) {
        // 3Dオブジェクト（立方体）を作成
        Box box = new Box(100, 100, 100);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.DARKCYAN);
        box.setMaterial(material);

        // 回転アニメーションを追加
        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        box.getTransforms().addAll(rotateX, rotateY);

        // JavaFXアニメーションタイマーを使用して回転させる
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                rotateX.setAngle(rotateX.getAngle() + 0.5);
                rotateY.setAngle(rotateY.getAngle() + 0.5);
            }
        };
        timer.start();

        // グループとカメラを設定
        Group root = new Group(box);
        Scene scene = new Scene(root, 600, 400, true);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-500);
        scene.setCamera(camera);

        // ステージの設定
        stage.setTitle("3D Cube Example");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

