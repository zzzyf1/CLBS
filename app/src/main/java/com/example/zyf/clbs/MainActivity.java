package com.example.zyf.clbs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.zyf.clbs.entity.Location;
import com.example.zyf.clbs.util.toJson;

import java.util.ArrayList;
import java.util.List;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class MainActivity extends AppCompatActivity {
    private TextView Longtitude;
    private TextView Latitude;
    public LocationClient mLocationClient;
    private MyLocationListener myListener=new MyLocationListener();
    public LocationClientOption option=new LocationClientOption();
    private MapView mapView=null;
    private BaiduMap baiduMap=null;
    private Button send;
    private Location BusLocation =new Location();

    //构建Marker坐标
    public BitmapDescriptor bitmapDescriptor;
    public OverlayOptions overlayOptions;
    public Marker marker;

    //Stomp客户端向服务器实时上传公交车位置
    private StompClient stompClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        SDKInitializer.setCoordType(CoordType.BD09LL);
        setContentView(R.layout.activity_main);

        send=findViewById(R.id.button);
        bitmapDescriptor=BitmapDescriptorFactory.fromResource(R.drawable.marker1);
        mapView=findViewById(R.id.mapView);
        baiduMap=mapView.getMap();
        //开启交通图 图层
        baiduMap.setTrafficEnabled(true);

        //连接服务器端
        createStompClient();


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stompClient.send("/app/welcome17", "My first STOMP message!").subscribe();
            }
        });

        /***
         * 定位参数配置
         */
        Longtitude=findViewById(R.id.longtitude);
        Latitude=findViewById(R.id.latitude);
        //声明LocationCLient类
        mLocationClient=new LocationClient(getApplicationContext());
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        //设置定位SDK参数
        //定位精度
        //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
         //LocationMode. Device_Sensors：仅使用设备；
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //返回坐标类型
        option.setCoorType("BD09LL");
        //连续定位,>=1000ms
        option.setScanSpan(10000);
        //设置是否使用GPS
        option.setOpenGps(true);
        //设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setLocationNotify(true);
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(false);
        //设置是否手机crash信息
        option.SetIgnoreCacheException(false);
        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        option.setWifiCacheTimeOut(5*60*1000);
        //设置option参数
        mLocationClient.setLocOption(option);
        /**
         * 动态权限申请
         */
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_WIFI_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CHANGE_WIFI_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.INTERNET)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.INTERNET);
        }
        //申请未添加的权限
        if(!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            //开始定位
            requestPosition();
        }



    }
    //请求坐标
    public void requestPosition(){
        mLocationClient.start();
    }
    //权限申请回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(MainActivity.this,"必须开启上述权限",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                    requestPosition();
                }else{
                    Toast.makeText(MainActivity.this,"未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
                default:
        }
    }
    //定位监听回调
    public class MyLocationListener extends BDAbstractLocationListener{

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            double latitude=bdLocation.getLatitude();
            double longtitude=bdLocation.getLongitude();
            //获取定位精度
            float radius=bdLocation.getRadius();
            //获取经纬度坐标类型，以LocationClientOption中设置的参数为准
            String coorType=bdLocation.getCoorType();
            //获取定位类型,定位错误返回码
            int errorCode=bdLocation.getLocType();
            //将经纬度显示到屏幕上
            Longtitude.setText("经度："+longtitude);
            Latitude.setText("纬度："+latitude);
            //地图标记
            LatLng point=new LatLng(latitude,longtitude);
            if(marker==null){
                overlayOptions=new MarkerOptions().position(point).icon(bitmapDescriptor).perspective(true);
                marker=(Marker)baiduMap.addOverlay(overlayOptions);
                //动态改变地图到指定经纬度
                MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLngZoom(point,15);
                baiduMap.animateMapStatus(mapStatusUpdate);
            }else{
                //动态监听汽车位置，显示到地图上
                marker.setPosition(point);
                //位置实体
                BusLocation.setLatitude(latitude);
                BusLocation.setLongitude(longtitude);
                //实时将位置信息上传到服务器端
                //平均间隔10s
                stompClient.send("/app/welcome17", toJson.locationToJson(BusLocation)).subscribe();
            }
        }
    }
    //创建StompClient，连接到服务器并监听其生命周期
    public void createStompClient(){
        //本地IP 192.168.68.217
        //公网IP 47.96.164.222
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://47.96.164.222:8080/hello/websocket");
        stompClient.withClientHeartbeat(1000).withServerHeartbeat(1000);
        stompClient.connect();
        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {

                case OPENED:
                    Log.d("MainActivity", "Stomp connection opened");
                    break;

                case ERROR:
                    Log.e("MainActivity", "Error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    Log.d("MainActivity", "Stomp connection closed");
                    break;
            }
        });
    }
    //

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(stompClient!=null){
            stompClient.disconnect();
        }
    }
}
