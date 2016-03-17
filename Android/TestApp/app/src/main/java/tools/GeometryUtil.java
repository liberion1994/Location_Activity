package tools;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import models.Record;

/**
 * Created by apple on 16/2/14.
 */
public class GeometryUtil {

    public static double TOLERANCE = 0.5;

    /**
     * 求B点经纬度
     * @param A 已知点的经纬度，
     * @param distance   AB两地的距离  单位km
     * @param angle  AB连线与正北方向的夹角（0~360）
     * @return  B点的经纬度
     */
    public static MyLatLng getMyLatLng(MyLatLng A,double distance,double angle){

        double dx = distance*1000*Math.sin(Math.toRadians(angle));
        double dy= distance*1000*Math.cos(Math.toRadians(angle));

        double bjd=(dx/A.Ed+A.m_RadLo)*180./Math.PI;
        double bwd=(dy/A.Ec+A.m_RadLa)*180./Math.PI;
        return new MyLatLng(bjd, bwd);
    }

    /**
     * 获取AB连线与正北方向的角度
     * @param A  A点的经纬度
     * @param B  B点的经纬度
     * @return  AB连线与正北方向的角度（0~360）
     */
    public static double getAngle(MyLatLng A,MyLatLng B){
        if (A.m_Longitude == B.m_Longitude && A.m_Latitude == B.m_Latitude)
            return 0;
        double dx=(B.m_RadLo-A.m_RadLo)*A.Ed;
        double dy=(B.m_RadLa-A.m_RadLa)*A.Ec;
        double angle=0.0;
        angle=Math.atan(Math.abs(dx/dy))*180./Math.PI;
        double dLo=B.m_Longitude-A.m_Longitude;
        double dLa=B.m_Latitude-A.m_Latitude;
        if(dLo>0&&dLa<=0){
            angle=(90.-angle)+90;
        }
        else if(dLo<=0&&dLa<0){
            angle=angle+180.;
        }else if(dLo<0&&dLa>=0){
            angle= (90.-angle)+270;
        }
        return angle;
    }

    public static class MyLatLng {
        final static double Rc=6378137;
        final static double Rj=6356725;
        double m_LoDeg,m_LoMin,m_LoSec;
        double m_LaDeg,m_LaMin,m_LaSec;
        double m_Longitude,m_Latitude;
        double m_RadLo,m_RadLa;
        double Ec;
        double Ed;
        public MyLatLng(double latitude, double longitude) {
            m_LoDeg=(int)longitude;
            m_LoMin=(int)((longitude-m_LoDeg)*60);
            m_LoSec=(longitude-m_LoDeg-m_LoMin/60.)*3600;

            m_LaDeg=(int)latitude;
            m_LaMin=(int)((latitude-m_LaDeg)*60);
            m_LaSec=(latitude-m_LaDeg-m_LaMin/60.)*3600;

            m_Longitude=longitude;
            m_Latitude=latitude;
            m_RadLo=longitude*Math.PI/180.;
            m_RadLa=latitude*Math.PI/180.;
            Ec=Rj+(Rc-Rj)*(90.-m_Latitude)/90.;
            Ed=Ec*Math.cos(m_RadLa);
        }
    }

    public static class MyCircle {
        public LatLng center;
        public double radius;

        public MyCircle(LatLng center, double radius) {
            this.center = center;
            this.radius = radius;
        }

        public boolean contains(LatLng point) {
            if (AMapUtils.calculateLineDistance(point, center) <= radius + TOLERANCE)
                return true;
            return false;
        }

        public boolean contains(Record record) {
            if (AMapUtils.calculateLineDistance(new LatLng(record.getLatitude(), record.getLongitude()),
                    center) <= radius + record.getAccuracy() + TOLERANCE)
                return true;
            return false;
        }
    }

    private static LatLng getCenter(LatLng p1, LatLng p2, LatLng p3) {

        double a1 = p2.longitude - p1.longitude;
        double b1 = p2.latitude - p1.latitude;
        double c1 = (a1 * a1 + b1 * b1) / 2;

        double a2 = p3.longitude - p1.longitude;
        double b2 = p3.latitude - p1.latitude;
        double c2 = (a2 * a2 + b2 * b2) / 2;

        double d = a1 * b2 - a2 * b1;
        if (d != 0) {
            double lng = p1.longitude + (c1 * b2 - c2 * b1) / d;
            double lat = p1.latitude + (a1 * c2 - a2 * c1) / d;
            return new LatLng(lat, lng);
        } else {
            //3 points in a line, and p3 should be father from p1 than p2
            return new LatLng((p1.latitude + p3.latitude) / 2, (p1.longitude + p3.longitude) / 2);
        }
    }

    public static MyCircle getMinCoverCircle(List<LatLng> points) {
        int len = points.size();
        if (len <= 1) {
            return null;
        }
        double radius = 0;
        LatLng center = new LatLng(points.get(0).latitude, points.get(0).longitude);
        for (int i = 1; i < len; i ++) {
            if (AMapUtils.calculateLineDistance(center, points.get(i)) > radius + TOLERANCE) {
                center = new LatLng(points.get(i).latitude, points.get(i).longitude);
                radius = 0;
                for (int j = 0; j < i; j ++) {
                    if (AMapUtils.calculateLineDistance(center, points.get(j)) > radius + TOLERANCE) {
                        center = new LatLng((points.get(i).latitude + points.get(j).latitude) / 2,
                                (points.get(i).longitude + points.get(j).longitude) / 2);
                        radius = AMapUtils.calculateLineDistance(points.get(i), points.get(j)) / 2;
                        for(int k = 0; k < j; k ++) {
                            if (AMapUtils.calculateLineDistance(center, points.get(k)) > radius + TOLERANCE) {
                                center = getCenter(points.get(i), points.get(j), points.get(k));
                                radius = AMapUtils.calculateLineDistance(center, points.get(k));
                            }
                        }
                    }
                }
            }
        }
        return new MyCircle(center, radius);
    }
}