package com.example.testgame;


import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl.IOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.PathModifier;
import org.andengine.entity.modifier.RotationModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.modifier.PathModifier.IPathModifierListener;
import org.andengine.entity.modifier.PathModifier.Path;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.ease.EaseSineInOut;

import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.widget.Toast;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class MainActivity extends SimpleBaseGameActivity{

	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;

	private BitmapTextureAtlas mPlayerTextureAtlas;
	private TiledTextureRegion mPlayerTextureRegion;
	
	private BitmapTextureAtlas mBulletTextureAtlas;
	private ITextureRegion bulletRegion;
	
	private BitmapTextureAtlas mButtonTexture;
	private ITextureRegion mButtonTextureRegion;
	
	private BitmapTextureAtlas mEnemyTextureAtlas;
	private TiledTextureRegion mEnemyTextureRegion;
	
	private Camera mCamera;
	
	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;
	private DigitalOnScreenControl mDigitalOnScreenControl;
	AnimatedSprite player;
	AnimatedSprite enemy;
	Sprite bullet;
	private PhysicsWorld mPhysicsWorld;
	
	

	Scene mScene = new Scene();
	
	private boolean direct;
	
	Rectangle rectangle;
	
	private Body playerbody;
	private Body bulletbody;
		
	
	@SuppressWarnings("unused")
	private boolean mPlaceOnScreenControlsAtDifferentVerticalLocations = false;
	
	@Override
	public EngineOptions onCreateEngineOptions() {
		// TODO Auto-generated method stub
		mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		
		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
		engineOptions.getTouchOptions().setNeedsMultiTouch(true);

		if(MultiTouch.isSupported(this)) {
			if(MultiTouch.isSupportedDistinct(this)) {
				Toast.makeText(this, "MultiTouch detected --> Both controls will work properly!", Toast.LENGTH_SHORT).show();
			} else {
				this.mPlaceOnScreenControlsAtDifferentVerticalLocations = true;
				Toast.makeText(this, "MultiTouch detected, but your device has problems distinguishing between fingers.\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
		}

		return engineOptions;	}

	@Override
	protected void onCreateResources() {
		// TODO Auto-generated method stub
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("files/");
		
		this.mPlayerTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 300, 128, TextureOptions.BILINEAR);
		this.mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mPlayerTextureAtlas, this, "player.png", 0, 0, 3, 4);
		this.mPlayerTextureAtlas.load();
		
		this.mEnemyTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 300, 128, TextureOptions.BILINEAR);
		this.mEnemyTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mEnemyTextureAtlas, this, "enemy.png", 0, 0, 3, 4);
		this.mEnemyTextureAtlas.load();
		
		this.mBulletTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 50, 50, TextureOptions.BILINEAR);
		this.bulletRegion=BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBulletTextureAtlas, this,"arrow.png", 0, 0);
		this.mBulletTextureAtlas.load();
		
		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
		this.mOnScreenControlTexture.load();
		
		this.mButtonTexture=new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mButtonTextureRegion=BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mButtonTexture, this, "onscreen_control_knob.png", 0, 0);
		this.mButtonTexture.load();		
		
	}

	@Override
	protected Scene onCreateScene() {
		// TODO Auto-generated method stub
		this.mEngine.registerUpdateHandler(new FPSLogger());
			
		mScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
			
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, 40), false);
		
		initPlayer();
		initBounds();
		initControls();
		
		
			
		
		mScene.registerUpdateHandler(this.mPhysicsWorld);

		return mScene;
		
		
	}
	
	protected void jump() {
		// TODO Auto-generated method stub
		int x=0;
		if(direct){
			x=7;
			
		}
		else{
			x=-7;
		}
		final Vector2 velocity = Vector2Pool.obtain(x, 50);
		playerbody.setLinearVelocity(velocity);
		Vector2Pool.recycle(velocity);
		
	    
  	  
  	   
	}
	
	public void fire() {
	     
		
        float startBulletX=player.getX()+50;
        float startBulletY=player.getY(); 
        int x=0;
        if(direct){
    	   x=10;
       }else{
    	   startBulletX=player.getX()-50;
    	   x=-10;}
        
       
        bullet=new Sprite(startBulletX, startBulletY, bulletRegion, this.getVertexBufferObjectManager());
        if(direct){
     	   
        }else{
     	   bullet.setRotation(180);
     	   }
        
        final FixtureDef bulletFixtureDef1 = PhysicsFactory.createFixtureDef(0, 0, 0);
        this.bulletbody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, bullet, BodyType.KinematicBody, bulletFixtureDef1);
        
        Vector2 velocity1=new Vector2(x, 0);
        this.bulletbody.setLinearVelocity(velocity1);
        Vector2Pool.recycle(velocity1);
        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(bullet, this.bulletbody, true, false));
 
        this.mScene.attachChild(bullet);
        
        mScene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				if(bullet.collidesWith(enemy)) {
					bullet.setVisible(false);
					//enemy.setCurrentTileIndex(pCurrentTileIndex);
					enemy.setRotation(90);
					enemy.stopAnimation();
				} else {
					
				}
				
				
			}
		});
        
    }
	
	
	private void initPlayer(){
		
		final float playerX = (CAMERA_WIDTH - this.mPlayerTextureRegion.getWidth()) / 2;
		player = new AnimatedSprite(playerX-200,  3, this.mPlayerTextureRegion, this.getVertexBufferObjectManager());
		player.setScaleCenterY(this.mPlayerTextureRegion.getHeight());
		player.setScale(3.5f);
		
		enemy = new AnimatedSprite(playerX+500, CAMERA_HEIGHT - 40, this.mEnemyTextureRegion, this.getVertexBufferObjectManager());
		enemy.setScaleCenterY(this.mEnemyTextureRegion.getHeight());
		enemy.setScale(3.5f);
		
		
		
		final FixtureDef FixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0);
		playerbody = PhysicsFactory.createBoxBody(mPhysicsWorld, player, BodyType.DynamicBody, FixtureDef);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(player, playerbody, true, false));
		mCamera.setChaseEntity(player);
		mScene.attachChild(player);
		
		
		
		final Path path = new Path(2).to(0, CAMERA_HEIGHT - 40).to(playerX+500, CAMERA_HEIGHT - 40);

		
		enemy.registerEntityModifier(new LoopEntityModifier(new PathModifier(30, path, null, new IPathModifierListener() {
			@Override
			public void onPathStarted(final PathModifier pPathModifier, final IEntity pEntity) {
				Debug.d("onPathStarted");
			}

			@Override
			public void onPathWaypointStarted(final PathModifier pPathModifier, final IEntity pEntity, final int pWaypointIndex) {
				Debug.d("onPathWaypointStarted:  " + pWaypointIndex);
				switch(pWaypointIndex) {
					case 0:
						enemy.animate(new long[]{200, 200, 200}, 6, 8, true);
						break;
					case 1:
						enemy.animate(new long[]{200, 200, 200}, 3, 5, true);
						break;
					case 2:
						enemy.animate(new long[]{200, 200, 200}, 0, 2, true);
						break;
					case 3:
						enemy.animate(new long[]{200, 200, 200}, 9, 11, true);
						break;
				}
			}

			@Override
			public void onPathWaypointFinished(final PathModifier pPathModifier, final IEntity pEntity, final int pWaypointIndex) {
				Debug.d("onPathWaypointFinished: " + pWaypointIndex);
			}

			@Override
			public void onPathFinished(final PathModifier pPathModifier, final IEntity pEntity) {
				Debug.d("onPathFinished");
			}
		}, EaseSineInOut.getInstance())));
		
		mScene.attachChild(enemy);
	}
	
	private void initBounds(){
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT-2 , CAMERA_WIDTH*3, 2, this.getVertexBufferObjectManager());		 
		ground.setColor(0, 0, 0);
		FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		this.mScene.attachChild(ground);
	}
	
	public void initControls(){
		
		this.mDigitalOnScreenControl = new DigitalOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new IOnScreenControlListener() {
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				Vector2 velocity=new Vector2(pValueX*5, 0);
					playerbody.setLinearVelocity(velocity);
					Vector2Pool.recycle(velocity);		
										
				if(!player.isAnimationRunning() && !player.isAnimationRunning())
	                if(pValueX>0 && !player.isAnimationRunning()){//Derecha
	                    direct=true;
	                	player.animate(new long[]{100, 100, 100}, 3, 5, false);
	                }else{
	                    if(pValueX<0){//Izquierda
	                        direct=false;
	                    	player.animate(new long[]{100, 100, 100}, 9, 11, false);
	                    }
	               
	                }
				
				
			}
		
			
			
		});
		
				
		this.mDigitalOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		this.mDigitalOnScreenControl.getControlBase().setAlpha(0.5f);
		this.mDigitalOnScreenControl.getControlBase().setScaleCenter(0, 128);
		this.mDigitalOnScreenControl.getControlBase().setScale(1.25f);
		this.mDigitalOnScreenControl.getControlKnob().setScale(1.25f);
		this.mDigitalOnScreenControl.refreshControlKnobPosition();
		
		
		ButtonSprite button = new  ButtonSprite(CAMERA_WIDTH-170, CAMERA_HEIGHT-70, mButtonTextureRegion, this.getVertexBufferObjectManager()) {

			public boolean onAreaTouched(TouchEvent pTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
		           if(pTouchEvent.isActionDown()) {
		        	   if(player.getY() > 5){//ground.getY()-95){
		        		   jump();
		        		   
		        		   
		        	   }
		        	   else{
		        		   
		        	   }
		        			           }
		           return super.onAreaTouched(pTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
		       }
		};
		button.setAlpha(0.5f);
		button.setScale(1.5f);
		
		
		ButtonSprite fire = new  ButtonSprite(CAMERA_WIDTH-85, CAMERA_HEIGHT-130, mButtonTextureRegion, this.getVertexBufferObjectManager()) {

			public boolean onAreaTouched(TouchEvent pTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
		           if(pTouchEvent.isActionDown()) {
		        	   fire();
		        			           }
		           return super.onAreaTouched(pTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
		       }
		};
		fire.setAlpha(0.5f);
		fire.setScale(1.5f);
		
		
		mScene.setChildScene(this.mDigitalOnScreenControl);
		
		mDigitalOnScreenControl.attachChild(button);
		mDigitalOnScreenControl.registerTouchArea(button);
		mDigitalOnScreenControl.attachChild(fire);
		mDigitalOnScreenControl.registerTouchArea(fire);
		
	}
	
	
	    
}
