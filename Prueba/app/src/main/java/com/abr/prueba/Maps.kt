package com.abr.prueba

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class Maps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map

        //Punto de prueba (SABANA)
        val bogota = LatLng(4.8703, -74.0326)
        gMap.addMarker(MarkerOptions().position(bogota).title("SABANA"))
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 13f))

        val edificioA = LatLng(4.860938051175516, -74.03450502880939)
        gMap.addMarker(MarkerOptions().position(edificioA).title("Edificio A"))

        val edificioB = LatLng(4.8607074971693205, -74.03408018858276)
        gMap.addMarker(MarkerOptions().position(edificioB).title("Edificio B"))

        val edificioC = LatLng(4.8605109592656826, -74.0346169286905)
        gMap.addMarker(MarkerOptions().position(edificioC).title("Edificio C"))

        val edificioD = LatLng(4.861220, -74.032881)
        gMap.addMarker(MarkerOptions().position(edificioD).title("Edificio D"))

        val edificioE1 = LatLng(4.860532071773142, -74.0313476171924)
        gMap.addMarker(MarkerOptions().position(edificioE1).title("Edificio E1"))

        val edificioE2 = LatLng(4.860655675958997, -74.03121680051751)
        gMap.addMarker(MarkerOptions().position(edificioE2).title("Edificio E2"))

        val edificioF = LatLng(4.859644664242708, -74.03282947340118)
        gMap.addMarker(MarkerOptions().position(edificioF).title("Edificio F"))

        val edificioG = LatLng(4.861478205062187, -74.03173330083506)
        gMap.addMarker(MarkerOptions().position(edificioG).title("Edificio G"))

        val edificioH = LatLng(4.860056109382236, -74.0327217990918)
        gMap.addMarker(MarkerOptions().position(edificioH).title("Edificio H"))

        val edificioK = LatLng(4.862318550207407, -74.0338636702441)
        gMap.addMarker(MarkerOptions().position(edificioK).title("Edificio K"))

        val edificioO = LatLng(4.86088770696634, -74.03263271033947)
        gMap.addMarker(MarkerOptions().position(edificioO).title("Edificio O"))

        val cafeBolsa = LatLng(4.862787795757815, -74.03326328391614)
        gMap.addMarker(MarkerOptions().position(cafeBolsa).title("Cafe Bolsa"))

        val livingLab = LatLng(4.8624540824847715, -74.03210063751898)
        gMap.addMarker(MarkerOptions().position(livingLab).title("Living Lab"))

        val biblioteca = LatLng(4.86114181375747, -74.0319665201184)
        gMap.addMarker(MarkerOptions().position(biblioteca).title("Biblioteca"))

        val atelier = LatLng(4.859765, -74.032918)
        gMap.addMarker(MarkerOptions().position(atelier).title("Atelier"))

        val fabLab = LatLng(4.853676127674738, -74.02603891519259)
        gMap.addMarker(MarkerOptions().position(fabLab).title("Fab Lab"))
    }
}