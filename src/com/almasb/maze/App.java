package com.almasb.maze;

import com.almasb.maze.MazeGenerator.MazeCell;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.AmbientLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.TangentBinormalGenerator;

public class App extends SimpleApplication {

    private BulletAppState physicsState;
    private Player player;

    private SpotLight flashlight;

    @Override
    public void simpleInitApp() {
        registerAssetsLocation();

        initLight();
        initPhysics();

        // 10 x 10 = 100 square blocks
        int mazeSize = 10;
        // half wall size, so 6*2 = 12
        int wallSize = 6;

        initFloor(mazeSize, wallSize);
        initMaze(mazeSize, wallSize);
        initPlayer(mazeSize, wallSize);
    }

    private void registerAssetsLocation() {
        assetManager.registerLocator(getClass().getResource("/assets/").toExternalForm(),
                UrlLocator.class);
    }

    private void initLight() {
        AmbientLight globalLight = new AmbientLight();
        globalLight.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(globalLight);

        flashlight = new SpotLight();
        flashlight.setColor(ColorRGBA.White.mult(1.5f));
        flashlight.setSpotRange(75);
        flashlight.setSpotInnerAngle(5 * FastMath.DEG_TO_RAD);
        flashlight.setSpotOuterAngle(15 * FastMath.DEG_TO_RAD);
        rootNode.addLight(flashlight);
    }

    private void initPhysics() {
        physicsState = new BulletAppState();
        stateManager.attach(physicsState);
    }

    private void initMaze(int mazeSize, int wallSize) {
        MazeCell[][] maze = new MazeGenerator(mazeSize, mazeSize).getMaze();

        Node mazeNode = new Node("Maze");

        // build wall geometry
        Box wallMesh = new Box(wallSize, 6f, 0.5f);
        Geometry wallGeo = new Geometry("Wall", wallMesh);
        TangentBinormalGenerator.generate(wallMesh);           // to use normal maps

        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        wallMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/wall.jpg"));
        wallMat.setTexture("NormalMap", assetManager.loadTexture("Textures/wall_normals.jpg"));
        wallMat.setBoolean("UseMaterialColors", true);
        wallMat.setColor("Diffuse", ColorRGBA.White);
        wallMat.setColor("Specular", ColorRGBA.White);
        wallMat.setColor("Ambient", ColorRGBA.White);
        wallMat.setFloat("Shininess", 32);  // [0,128]

        wallGeo.setMaterial(wallMat);
        wallGeo.setShadowMode(ShadowMode.CastAndReceive);


        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                MazeCell cell = maze[j][i];
                if (cell.leftWall) {
                    Geometry wall = wallGeo.clone();
                    wall.rotate(0, 90 * FastMath.DEG_TO_RAD, 0);
                    wall.setLocalTranslation(j*wallSize*2 - wallSize, 6f,  i*wallSize*2 + wallSize);

                    mazeNode.attachChild(wall);
                }

                if (cell.topWall) {
                    Geometry wall = wallGeo.clone();
                    wall.setLocalTranslation(j*wallSize*2, 6f, i*wallSize*2);

                    mazeNode.attachChild(wall);
                }

                // right side
                if (j == mazeSize - 1) {
                    Geometry wall = wallGeo.clone();
                    wall.rotate(0, 90 * FastMath.DEG_TO_RAD, 0);
                    wall.setLocalTranslation(j*wallSize*2 + wallSize, 6f,  i*wallSize*2 + wallSize);

                    mazeNode.attachChild(wall);
                }
            }
        }

        // bottom of the maze
        for (int j = 0; j < mazeSize; j++) {
            Geometry wall = wallGeo.clone();
            wall.setLocalTranslation(j*wallSize*2, 6f, mazeSize*wallSize*2);

            mazeNode.attachChild(wall);
        }

        // register maze for collision
        CollisionShape mazeShape = CollisionShapeFactory.createMeshShape(mazeNode);
        RigidBodyControl mazePhysicsBody = new RigidBodyControl(mazeShape, 0);
        mazeNode.addControl(mazePhysicsBody);

        physicsState.getPhysicsSpace().add(mazePhysicsBody);

        // add maze for render
        rootNode.attachChild(mazeNode);
    }

    private void initFloor(int mazeSize, int wallSize) {
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 8);

        Node floorNode = new Node("Floor");

        Box floor = new Box(20, 1f, 20);
        TangentBinormalGenerator.generate(floor);
        floor.scaleTextureCoordinates(new Vector2f(10, 10));
        Geometry floorGeom = new Geometry("Tile", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setShadowMode(ShadowMode.Receive);

        for (int i = 0; i < wallSize * 2 * mazeSize / 40 + 1; i++) {
            for (int j = 0; j < wallSize * 2 * mazeSize / 40 + 1; j++) {
                Spatial tile = floorGeom.clone();
                tile.setLocalTranslation(20*j*2, -1, 20*i*2);

                floorNode.attachChild(tile);
            }
        }

        rootNode.attachChild(floorNode);
    }

    private void initPlayer(int mazeSize, int wallSize) {
        player = new Player(mazeSize * wallSize, 3, mazeSize * wallSize + wallSize, inputManager, cam);
        physicsState.getPhysicsSpace().add(player);

        flyCam.setMoveSpeed(100);
    }

    @Override
    public void simpleUpdate(float tpf) {
        player.onUpdate();

        flashlight.setDirection(cam.getDirection());
        flashlight.setPosition(cam.getLocation());
    }
}
