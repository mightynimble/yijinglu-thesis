/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.math;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.math.Math;
import umd.lu.thesis.pums2010.objects.ModeChoice;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class Math /* extends umd.lu.thesis.simulation.app2000.math.Formulae */ {

    private static final int INVALID_QUARTER = -1;

    private final HashMap<String, Double[]> otherCarMap;

    private final HashMap<String, Double[]> businessCarMap;

    private final HashMap<String, Double[]> airMap;

    private final HashMap<String, Double[]> trainMap;

    private final HashMap<String, Double[]> quarterAirMap;

    // <msapmsa, [zoneId, dumMsa]>
    private final HashMap<Integer, Integer[]> zonIdMap;

    private final HashMap<Integer, Integer> idMsaMap;

    // <o/d, [emp, hh]>    
    private final HashMap<Integer, Double[]> msaEmpMap;

    private static final Logger sLog = LogManager.getLogger(Math.class);

    public static final int alt = 380;

    /**
     * Used to temporarily store the sum of exp of uDs to prevent duplicate
     * calculation.
     */
//    private LinkedHashMap<String, Double> UD_EXP_SUM_BUFFER;
//    private static final int UD_EXP_SUM_BUFFER_SIZE = 10000;
    /**
     * Used to store all pre-calculated logsum.
     * @see LogSum.java - the value of logsum can be calculated from
     *  - o [1-380]
     *  - d [1-380]
     *  - trip purpose [business, pb, pleasure]
     *  - quarter [-1, 1, 2, 3, 4]
     */
    private HashMap<String, Double> logsumCacheQ0;

    private HashMap<String, Double> logsumCacheQ1;

    private HashMap<String, Double> logsumCacheQ2;

    private HashMap<String, Double> logsumCacheQ3;

    private HashMap<String, Double> logsumCacheQ4;

    private static final Map<String, Double> destCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put(TripType.BUSINESS.name() + "-lgs", 0.9989822);
        aMap.put(TripType.BUSINESS.name() + "-dist", -0.0027291);
        aMap.put(TripType.BUSINESS.name() + "-sqDist", 0.0009215);
        aMap.put(TripType.BUSINESS.name() + "-trDist", -0.0000112);
        aMap.put(TripType.BUSINESS.name() + "-msa", -0.6954229);
        aMap.put(TripType.BUSINESS.name() + "-emp", 0.002333);
        aMap.put(TripType.BUSINESS.name() + "-hh", -0.0026529);
        aMap.put(TripType.BUSINESS.name() + "-lv", -2.178784);

        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-lgs", 0.6089201);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-dist", -0.0036128);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-sqDist", 0.0007052);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-msa", -0.9645431);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-emp", 0.0013741);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-hh", -0.0011477);

        aMap.put(TripType.PLEASURE.name() + "-lgs", 0.4242957);
        aMap.put(TripType.PLEASURE.name() + "-dist", -0.0033921);
        aMap.put(TripType.PLEASURE.name() + "-sqDist", 0.0006737);
        aMap.put(TripType.PLEASURE.name() + "-msa", -1.255553);
        aMap.put(TripType.PLEASURE.name() + "-emp", 0.0016221);
        aMap.put(TripType.PLEASURE.name() + "-hh", -0.0016747);
        aMap.put(TripType.PLEASURE.name() + "-fl", 1.974002);
        aMap.put(TripType.PLEASURE.name() + "-lv", -1.821725);

        destCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toyBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_Lgs", 0.0504287);
        aMap.put("Coef_Inc_1", 8.02e-06);
        aMap.put("Coef_Inc_2", -1.39e-06);
        aMap.put("Coef_Inc_3", 3.61e-06);
        aMap.put("Coef_emp_1", 0.5858141);
        aMap.put("Coef_emp_2", 0.2450074);
        aMap.put("Coef_emp_3", -0.3848681);
        aMap.put("Coef_hhchd_1", 0.3763038);
        aMap.put("Coef_hhchd_2", 0.2898711);
        aMap.put("Coef_hhchd_3", 0.0015771);
        aMap.put("Coef_Age_1", 0.0298472);
        aMap.put("Coef_Age_2", 0.0371919);
        aMap.put("Coef_Age_3", 0.0002595);
        aMap.put("Asc_1", -2.330421);
        aMap.put("Asc_2", -2.677752);
        aMap.put("Asc_3", -1.527069);

        toyBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toySimplePleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_Inc_1", 8.67e-06);
        aMap.put("Coef_Inc_2", 4.02e-06);
        aMap.put("Coef_Inc_3", 8.09e-07);
        aMap.put("Coef_emp_1", 0.308);
        aMap.put("Coef_emp_2", 0.458);
        aMap.put("Coef_emp_3", 0.236);
        aMap.put("Coef_unemp_1", 0.062);
        aMap.put("Coef_unemp_2", 0.374);
        aMap.put("Coef_unemp_3", 0.019);
        aMap.put("Coef_sig_1", 0.137);
        aMap.put("Coef_sig_2", 0.28);
        aMap.put("Coef_sig_3", 0.124);
        aMap.put("Coef_Age_1", 0.010);
        aMap.put("Coef_Age_2", 0.010);
        aMap.put("Coef_Age_3", 0.006);
        aMap.put("Coef_hhochd_1", 0.312);
        aMap.put("Coef_hhochd_2", 0.468);
        aMap.put("Coef_hhochd_3", 0.42);
        aMap.put("Coef_hhchd_1", 0.539);
        aMap.put("Coef_hhchd_2", 0.842);
        aMap.put("Coef_hhchd_3", 0.825);
        aMap.put("Asc_1", 0.119);
        aMap.put("Asc_2", -0.39);
        aMap.put("Asc_3", -0.005);


        toySimplePleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toyFullPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_lgs", 0.1472019);
        aMap.put("Coef_Inc_1", 8.76e-06);
        aMap.put("Coef_Inc_2", 4.38e-06);
        aMap.put("Coef_Inc_3", 7.20e-08);
        aMap.put("Coef_emp_1", 0.3755023);
        aMap.put("Coef_emp_2", 0.1551358);
        aMap.put("Coef_emp_3", 0.1543296);
        aMap.put("Coef_male_1", -0.2563758);
        aMap.put("Coef_male_2", -0.0662326);
        aMap.put("Coef_male_3", -0.3575725);
        aMap.put("Coef_Age_1", 0.0158632);
        aMap.put("Coef_Age_2", 0.009703);
        aMap.put("Coef_Age_3", 0.0064103);
        aMap.put("Coef_hhchd_1", 0.2226236);
        aMap.put("Coef_hhchd_2", 0.5444753);
        aMap.put("Coef_hhchd_3", 0.5383717);
        aMap.put("Asc_1", -1.330353);
        aMap.put("Asc_2", -0.8946058);
        aMap.put("Asc_3", -0.43011720);

        toyFullPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_MSA", -0.152);
        aMap.put("p_Cwc", 0.115);
        aMap.put("p_sf", -0.056);
        aMap.put("p_nfh", -0.158);
        aMap.put("p_size", 0.015);
        aMap.put("p_medinc", -0.015);
        aMap.put("p_higinc", -0.128);
        aMap.put("p_unemp", -0.147);
        aMap.put("p_student", -0.043);
        aMap.put("p_quart2", -0.246);
        aMap.put("p_quart3", -0.328);
        aMap.put("p_quart4", -0.140);
        aMap.put("p_age", 0.004);
        aMap.put("p_harf", -0.408);
        aMap.put("cons", -0.751);

        tdBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_cwc", 0.054);
        aMap.put("p_sf", -0.088);
        aMap.put("p_nfh", -0.145);
        aMap.put("p_size", 0.011);
        aMap.put("p_medinc", -0.051);
        aMap.put("p_higinc", -0.219);
        aMap.put("p_unemp", -0.334);
        aMap.put("p_student", -0.278);
        aMap.put("p_age", -0.006);
        aMap.put("p_harf", -0.150);
        aMap.put("cons", -0.731);

        tdPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdPBCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_MSA", -0.081);
        aMap.put("p_Cwc", 0.153);
        aMap.put("p_sf", -0.199);
        aMap.put("p_nfh", 0.142);
        aMap.put("p_size", 0.020);
        aMap.put("p_medinc", -0.083);
        aMap.put("p_higinc", -0.298);
        aMap.put("p_unemp", -0.225);
        aMap.put("p_student", -0.753);
        aMap.put("p_quart2", -0.064);
        aMap.put("p_quart3", -0.130);
        aMap.put("p_quart4", -0.065);
        aMap.put("p_age", 0.004);
        aMap.put("p_harf", -0.572);
        aMap.put("cons", -0.517);

        tdPBCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_msa_b", -0.187);
        aMap.put("tp2_sf_b", -1.207);
        aMap.put("tp2_cwc_b", -0.474);
        aMap.put("tp2_size_b", -0.003);
        aMap.put("tp2_linc_b", 0.66);
        aMap.put("tp2_minc_b", 0.347);
        aMap.put("tp2_age_b", 0.02);
        aMap.put("tp2_femal_b", 0.908);
        aMap.put("tp2_cons_b", -1.221);
        aMap.put("tp3_msa_b", -0.327);
        aMap.put("tp3_sf_b", -0.847);
        aMap.put("tp3_cwc_b", 0.05);
        aMap.put("tp3_size_b", -0.051);
        aMap.put("tp3_linc_b", 1.063);
        aMap.put("tp3_minc_b", 0.632);
        aMap.put("tp3_age_b", 0.007);
        aMap.put("tp3_femal_b", 0.984);
        aMap.put("tp3_cons_b", -2.052);
        aMap.put("tp4_msa_b", -0.248);
        aMap.put("tp4_sf_b", -0.187);
        aMap.put("tp4_cwc_b", 0.027);
        aMap.put("tp4_size_b", 0.261);
        aMap.put("tp4_linc_b", 1.161);
        aMap.put("tp4_minc_b", 0.606);
        aMap.put("tp4_age_b", -0.007);
        aMap.put("tp4_femal_b", 1.046);
        aMap.put("tp4_cons_b", -2.288);

        tpsBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_sf_p", -2.505);
        aMap.put("tp2_cwc_p", 0.088);
        aMap.put("tp2_size_p", -0.265);
        aMap.put("tp2_linc_p", 0.119);
        aMap.put("tp2_minc_p", 0.168);
        aMap.put("tp2_age_p", 0.023);
        aMap.put("tp2_femal_p", 0.023);
        aMap.put("tp2_cons_p", 1.058);
        aMap.put("tp3_sf_p", -1.754);
        aMap.put("tp3_cwc_p", 1.588);
        aMap.put("tp3_size_p", -0.184);
        aMap.put("tp3_linc_p", 0.199);
        aMap.put("tp3_minc_p", 0.256);
        aMap.put("tp3_age_p", 0.013);
        aMap.put("tp3_femal_p", 0.098);
        aMap.put("tp3_cons_p", -0.196);
        aMap.put("tp4_sf_p", -0.921);
        aMap.put("tp4_cwc_p", 1.370);
        aMap.put("tp4_size_p", 0.395);
        aMap.put("tp4_linc_p", 0.467);
        aMap.put("tp4_minc_p", 0.394);
        aMap.put("tp4_age_p", 0.016);
        aMap.put("tp4_femal_p", 0.118);
        aMap.put("tp4_cons_p", -1.682);

        tpsPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsPBCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_msa_pb", -0.086);
        aMap.put("tp2_sf_pb", -2.095);
        aMap.put("tp2_cwc_pb", 0.01);
        aMap.put("tp2_size_pb", -0.174);
        aMap.put("tp2_linc_pb", 0.382);
        aMap.put("tp2_minc_pb", 0.268);
        aMap.put("tp2_age_pb", 0.022);
        aMap.put("tp2_femal_pb", 0.283);
        aMap.put("tp2_cons_pb", 0.315);
        aMap.put("tp3_msa_pb", 0.0004);
        aMap.put("tp3_sf_pb", -1.385);
        aMap.put("tp3_cwc_pb", 0.619);
        aMap.put("tp3_size_pb", -0.017);
        aMap.put("tp3_linc_pb", 0.699);
        aMap.put("tp3_minc_pb", 0.335);
        aMap.put("tp3_age_pb", 0.012);
        aMap.put("tp3_femal_pb", 0.384);
        aMap.put("tp3_cons_pb", -0.988);
        aMap.put("tp4_msa_pb", -0.238);
        aMap.put("tp4_sf_pb", -0.911);
        aMap.put("tp4_cwc_pb", 0.842);
        aMap.put("tp4_size_pb", 0.27);
        aMap.put("tp4_linc_pb", 0.771);
        aMap.put("tp4_minc_pb", 0.394);
        aMap.put("tp4_age_pb", 0.008);
        aMap.put("tp4_femal_pb", 0.402);
        aMap.put("tp4_cons_pb", -1.519);

        tpsPBCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> mcCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost1", -0.0325);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost2", -0.00934);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost3", -0.00662);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost4", -0.00370);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost5", -0.00278);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_Time", -0.0356);
        aMap.put("ASC_" + TripType.BUSINESS.name() + "_Air", -0.440);
        aMap.put("ASC_" + TripType.BUSINESS.name() + "_Train", -2.93);

        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost1", -0.00947);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost2", -0.00434);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost3", -0.000900);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost4", -0.000335);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_Time", -0.0590);
        aMap.put("ASC_" + TripType.PLEASURE.name() + "_Air", -2.95);
        aMap.put("ASC_" + TripType.PLEASURE.name() + "_Train", -3.56);

        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost1", -0.0127);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost2", -0.00570);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost3", -0.00396);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost4", -0.00276);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost5", -0.00108);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_Time", -0.0328);
        aMap.put("ASC_" + TripType.PERSONAL_BUSINESS + "_Air", -1.49);
        aMap.put("ASC_" + TripType.PERSONAL_BUSINESS + "_Train", -3.75);

        mcCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopFreqCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("pi_dist1", 0.001);
        aMap.put("pi_dura1", 0.025);
        aMap.put("pi_party1", 0.034);
        aMap.put("pi_car1", 1.044);
        aMap.put("pi_busi1", 0.640);
        aMap.put("pi_plea1", 1.014);
        aMap.put("pi_quart2_1", -0.659);
        aMap.put("pi_quart3_1", -0.237);
        aMap.put("pi_quart4_1", -0.011);
        aMap.put("consi1", -9.078);
        aMap.put("pi_dist2", 0.0005);
        aMap.put("pi_dura2", 0.017);
        aMap.put("pi_party2", 0.0002);
        aMap.put("pi_car2", 2.309);
        aMap.put("pi_busi2", 0.201);
        aMap.put("pi_plea2", 0.177);
        aMap.put("pi_quart2_2", 0.06);
        aMap.put("pi_quart3_2", 0.11);
        aMap.put("pi_quart4_2", -0.398);
        aMap.put("consi2", -6.444);
        aMap.put("pi_dist3", 0.0002);
        aMap.put("pi_dura3", 0.015);
        aMap.put("pi_party3", -0.043);
        aMap.put("pi_car3", -3.09);
        aMap.put("pi_busi3", 0.365);
        aMap.put("pi_plea3", 0.185);
        aMap.put("pi_quart2_3", 1.42);
        aMap.put("pi_quart3_3", 1.298);
        aMap.put("pi_quart4_3", 1.582);
        aMap.put("consi3", -3.344);
        aMap.put("pi_dist4", 0.001);
        aMap.put("pi_dura4", 0.031);
        aMap.put("pi_party4", -0.025);
        aMap.put("pi_car4", -1.483);
        aMap.put("pi_busi4", 0.599);
        aMap.put("pi_plea4", 0.570);
        aMap.put("pi_quart2_4", 0.393);
        aMap.put("pi_quart3_4", 0.463);
        aMap.put("pi_quart4_4", 0.268);
        aMap.put("consi4", -6.1);
        aMap.put("po_dist1", 0.001);
        aMap.put("po_dura1", 0.023);
        aMap.put("po_party1", -0.003);
        aMap.put("po_car1", 0.975);
        aMap.put("po_busi1", -0.527);
        aMap.put("po_plea1", 0.727);
        aMap.put("po_quart2_1", -0.368);
        aMap.put("po_quart3_1", 0.045);
        aMap.put("po_quart4_1", -1.162);
        aMap.put("conso1", -8.481);
        aMap.put("po_dist2", 0.001);
        aMap.put("po_dura2", 0.017);
        aMap.put("po_party2", -0.004);
        aMap.put("po_car2", 3.283);
        aMap.put("po_busi2", 0.208);
        aMap.put("po_plea2", 0.434);
        aMap.put("po_quart2_2", 0.133);
        aMap.put("po_quart3_2", -0.001);
        aMap.put("po_quart4_2", -0.096);
        aMap.put("conso2", -7.512);
        aMap.put("po_dist3", 0.001);
        aMap.put("po_dura3", 0.02);
        aMap.put("po_party3", 0.013);
        aMap.put("po_car3", 0.185);
        aMap.put("po_busi3", 0.704);
        aMap.put("po_plea3", 0.611);
        aMap.put("po_quart2_3", 0.004);
        aMap.put("po_quart3_3", 0.094);
        aMap.put("po_quart4_3", -0.405);
        aMap.put("conso3", -6.326);
        aMap.put("po_dist4", 0.001);
        aMap.put("po_dura4", 0.022);
        aMap.put("po_party4", 0.005);
        aMap.put("po_car4", 0.983);
        aMap.put("po_busi4", 0.298);
        aMap.put("po_plea4", 0.628);
        aMap.put("po_quart2_4", 0.176);
        aMap.put("po_quart3_4", 0.563);
        aMap.put("po_quart4_4", -0.691);
        aMap.put("conso4", -8.052);
        stopFreqCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopTypeCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("pi_sec_b", -0.061);
        aMap.put("pi_thir_b", -0.019);
        aMap.put("pi_fou_b", 0.561);
        aMap.put("pi_pt_b", -1.735);
        aMap.put("pi_pbt_b", -1.319);
        aMap.put("pi_party_b", -0.01);
        aMap.put("pi_car_b", 0.824);
        aMap.put("pi_air_b", 0.872);
        aMap.put("conis_b", -1.92);
        aMap.put("pi_sec_pb", -0.083);
        aMap.put("pi_thir_pb", -0.111);
        aMap.put("pi_fou_pb", 1.719);
        aMap.put("pi_pt_pb", 1.193);
        aMap.put("pi_pbt_pb", 2.972);
        aMap.put("pi_party_pb", 0.067);
        aMap.put("pi_car_pb", 0.645);
        aMap.put("pi_air_pb", -0.273);
        aMap.put("conis_pb", -5.953);
        aMap.put("po_sec_b", -0.462);
        aMap.put("po_thir_b", -0.395);
        aMap.put("po_fou_b", -0.589);
        aMap.put("po_pt_b", -3.904);
        aMap.put("po_pbt_b", -2.697);
        aMap.put("po_party_b", -0.015);
        aMap.put("po_car_b", -0.067);
        aMap.put("po_air_b", 1.227);
        aMap.put("conos_b", 0.666);
        aMap.put("po_sec_pb", -0.061);
        aMap.put("po_thir_pb", -0.352);
        aMap.put("po_fou_pb", 0.048);
        aMap.put("po_pt_pb", 1.259);
        aMap.put("po_pbt_pb", 3.662);
        aMap.put("po_party_pb", -0.123);
        aMap.put("po_car_pb", -0.547);
        aMap.put("po_air_pb", 0.007);
        aMap.put("conos_pb", -3.939);
        stopTypeCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopLocCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("po_det_dist", -0.0081102);
        aMap.put("po_gtc_s", -0.1029191);
        aMap.put("po_gtc_m", -0.0001649);
        aMap.put("po_gtc_l", 0.0015895);
        aMap.put("po_gtc_lc", -0.0015207);
        aMap.put("po_emp", 0.0017037);
        aMap.put("po_hh", -0.0015843);
        aMap.put("po_nmsa", 1.82495);
        aMap.put("pi_det_dist", -0.0080677);
        aMap.put("pi_gtc_s", -0.0360756);
        aMap.put("pi_gtc_m", -0.0007487);
        aMap.put("pi_gtc_mc", 0.0010112);
        aMap.put("pi_gtc_l", -0.0000916);
        aMap.put("pi_emp", 0.000631);
        aMap.put("pi_hh", -0.0002232);
        aMap.put("pi_nmsa", 1.780490);
        stopLocCoefs = Collections.unmodifiableMap(aMap);
    }

    private LogSum logsum;

    public Math() {
        idMsaMap = new HashMap<>();
        zonIdMap = initZoneIdMapAndIdMsaMap();
        otherCarMap = initOtherCarMap();
        businessCarMap = initBusinessCarMap();
        airMap = initAirMap();
        trainMap = initTrainMap();
        quarterAirMap = initQuarterAirMap();
        msaEmpMap = initMsaEmpMap();

        logsum = new LogSum(trainMap, airMap, quarterAirMap, businessCarMap, otherCarMap);
//        UD_EXP_SUM_BUFFER = new LinkedHashMap<>();

        logsumCacheQ0 = new HashMap<>();
        logsumCacheQ1 = new HashMap<>();
        logsumCacheQ2 = new HashMap<>();
        logsumCacheQ3 = new HashMap<>();
        logsumCacheQ4 = new HashMap<>();

        preCalculateLogsum();
    }

    public double destUD(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        double dist = o == d ? Double.NEGATIVE_INFINITY : businessCarMap.get(getKey(o, d))[3];
        if(dist >= 50) {
            if(type == TripType.BUSINESS) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-trDist") * dist * dist * dist * 0.00001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1]
                       + destCoefs.get(type.name() + "-lv") * (d == 201 ? 1 : 0);
            }
            else if(type == TripType.PLEASURE) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1]
                       + destCoefs.get(type.name() + "-fl")
                         * (d == 55 || d == 90 || d == 124 || d == 126 || d == 171
                            || d == 194 || d == 229 || d == 270 || d == 274
                            || d == 321 || d == 343 || d == 344 || d == 365 ? 1 : 0)
                       + destCoefs.get(type.name() + "-lv") * (d == 201 ? 1 : 0);
            }
            else if(type == TripType.PERSONAL_BUSINESS) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1];
            }
        }
        else {
            return Double.NEGATIVE_INFINITY;
        }
        throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", person.ID: " + p.getPid() + ")");
    }

    // sum[(e^(u_d1 ) +e^(u_d2 )+....+ e^(u_dn )]
    public double destUDExpSum(Person2010 p, int o, TripType type, int quarter) {
        double sum = 0.0;
        try {
            for (int d = 1; d <= Math.alt; d++) {
                sum += exp(destUD(p, o, d, type, quarter));
            }
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return sum;
    }

    public double toyUDExpSum(Person2010 p, int o, int d, TripType type) {
        return toyUDExp(p, o, d, type, 1) + toyUDExp(p, o, d, type, 2) + toyUDExp(p, o, d, type, 3) + toyUDExp(p, o, d, type, 4);
    }

    public double destUDExp(Person2010 p, int o, int d, TripType type, int quarter) {
        try {
            return exp(destUD(p, o, d, type, quarter));
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return Double.NEGATIVE_INFINITY;
    }

    public double toyUDExp(Person2010 p, int o, int d, TripType type, int quarter) {
        try {
            return exp(toyUD(p, o, d, type, quarter));
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return Double.NEGATIVE_INFINITY;
    }

    public double toyUD(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if(type == TripType.BUSINESS) {
            if(quarter == 1) {
                return toyBusinessCoefs.get("Asc_1")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 1)
                       + toyBusinessCoefs.get("Coef_Inc_1") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_1") * p.getAge();

            }
            else if(quarter == 2) {
                return toyBusinessCoefs.get("Asc_2")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 2)
                       + toyBusinessCoefs.get("Coef_Inc_2") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_2") * p.getAge();
            }
            else if(quarter == 3) {
                return toyBusinessCoefs.get("Asc_3")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 3)
                       + toyBusinessCoefs.get("Coef_Inc_3") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_3") * p.getAge();
            }
            else {
                // quarter == 4
                return toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 4);
            }
        }
        else if(type == TripType.PLEASURE) {
            if(d == Integer.MIN_VALUE) {
                if(quarter == 1) {
                    return toySimplePleasureCoefs.get("Asc_1")
                           + toySimplePleasureCoefs.get("Coef_Inc_1") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_1") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_1") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_1") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_1") * (p.getHhType() == 1 ? 1 : 0);
                }
                else if(quarter == 2) {
                    return toySimplePleasureCoefs.get("Asc_2")
                           + toySimplePleasureCoefs.get("Coef_Inc_2") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_2") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_2") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_2") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_2") * (p.getHhType() == 1 ? 1 : 0);
                }
                else if(quarter == 3) {
                    return toySimplePleasureCoefs.get("Asc_3")
                           + toySimplePleasureCoefs.get("Coef_Inc_3") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_3") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_3") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_3") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_3") * (p.getHhType() == 1 ? 1 : 0);
                }
                else {
                    // quarter == 4
                    return 0.0;
                }
            }
            else {
                if(quarter == 1) {
                    return toyFullPleasureCoefs.get("Asc_1")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_1") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_male_1") * (p.getSex() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_1") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0);
                }
                else if(quarter == 2) {
                    return toyFullPleasureCoefs.get("Asc_2")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_2") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_male_2") * (p.getSex() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_2") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0);
                }
                else if(quarter == 3) {
                    return toyFullPleasureCoefs.get("Asc_3")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_3") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_male_3") * (p.getSex() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_3") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0);
                }
                else {
                    // quarter == 4
                    return toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter);
                }
            }
        }
        throw new InvalidValueException("Invalid TripType found. Only [BUSINESS, PLEASURR] is accepted here. [PB] doesn't need to calculate uD.");
    }

    private double tdHT(Person2010 p, int d, int toy, int t, TripType type) {
        if(type == TripType.BUSINESS) {//???
            return tdBusinessCoefs.get("p_MSA") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tdBusinessCoefs.get("p_Cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdBusinessCoefs.get("p_size") * p.getNp()
                   + tdBusinessCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart2") * (toy == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart3") * (toy == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart4") * (toy == 4 ? 1 : 0)
                   + tdBusinessCoefs.get("p_age") * (p.getAge())
                   + tdBusinessCoefs.get("p_harf") * log(t)
                   + tdBusinessCoefs.get("cons");
        }
        else if(type == TripType.PLEASURE) {
            return tdPleasureCoefs.get("p_cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdPleasureCoefs.get("p_size") * p.getNp()
                   + tdPleasureCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_age") * (p.getAge())
                   + tdPleasureCoefs.get("p_harf") * log(t)
                   + tdPleasureCoefs.get("cons");

        }
        else {
            // TripType.PERSONAL_BUSINESS
            return tdPBCoefs.get("p_MSA") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tdPBCoefs.get("p_Cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdPBCoefs.get("p_size") * p.getNp()
                   + tdPBCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_quart2") * (toy == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_quart3") * (toy == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_quart4") * (toy == 4 ? 1 : 0)
                   + tdPBCoefs.get("p_age") * (p.getAge())
                   + tdPBCoefs.get("p_harf") * log(t)
                   + tdPBCoefs.get("cons");
        }
    }

    private double tdPT(Person2010 p, int d, int toy, int t, TripType type) {
        return 1 / (1 + exp(0 - tdHT(p, d, toy, t, type)));
    }

    public double tdST(Person2010 p, int d, int toy, int t, TripType type) {
        double sT = 1.0;
        for (int i = 1; i <= t; i++) {
            sT *= (1 - tdPT(p, d, toy, i, type));
        }
        return sT;
    }

    private double tpsBusinessUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsBusinessCoefs.get("tp" + tps + "_msa_b") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tpsBusinessCoefs.get("tp" + tps + "_sf_b") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_cwc_b") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_size_b") * p.getNp()
                   + tpsBusinessCoefs.get("tp" + tps + "_linc_b") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_minc_b") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_age_b") * p.getAge()
                   + tpsBusinessCoefs.get("tp" + tps + "_femal_b") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsBusinessCoefs.get("tp" + tps + "_cons_b");
        }
    }

    private double tpsPleasureUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsPleasureCoefs.get("tp" + tps + "_sf_p") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_cwc_p") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_size_p") * p.getNp()
                   + tpsPleasureCoefs.get("tp" + tps + "_linc_p") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_minc_p") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_age_p") * p.getAge()
                   + tpsPleasureCoefs.get("tp" + tps + "_femal_p") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsPleasureCoefs.get("tp" + tps + "_cons_p");
        }
    }

    private double tpsPBUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsPBCoefs.get("tp" + tps + "_msa_pb") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tpsPBCoefs.get("tp" + tps + "_sf_pb") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_cwc_pb") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_size_pb") * p.getNp()
                   + tpsPBCoefs.get("tp" + tps + "_linc_pb") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_minc_pb") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_age_pb") * p.getAge()
                   + tpsPBCoefs.get("tp" + tps + "_femal_pb") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsPBCoefs.get("tp" + tps + "_cons_pb");
        }
    }

    public double tpsUtpExp(Person2010 p, int d, int tps, TripType type) {
        if(type == TripType.BUSINESS) {
            return exp(tpsBusinessUTP(p, d, tps));
        }
        else if(type == TripType.PLEASURE) {
            return exp(tpsPleasureUTP(p, d, tps));
        }
        else {
            return exp(tpsPBUTP(p, d, tps));
        }
    }

    public double mcUcarExp(Person2010 p, TripType type, int d, int o) {
        double tourCarCost = logsum.tourCarCost(p.getIncLevel(), o, d, type);
        if(tourCarCost == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 332 && tourCarCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 476 && tourCarCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourCarCost * (tourCarCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 312 && tourCarCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 312 && tourCarCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 436 && tourCarCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourCarCost * (tourCarCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
    }

    public double mcUairExp(Person2010 p, TripType type, int d, int o, int toy) {
        double tourAirCost = logsum.tourAirCost(o, d, toy);
        if(tourAirCost == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 332 && tourAirCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 476 && tourAirCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourAirCost * (tourAirCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 312 && tourAirCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 312 && tourAirCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 436 && tourAirCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourAirCost * (tourAirCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
    }

    public double mcUtrainExp(Person2010 p, TripType type, int d, int o) {
        double tourTrainCost = logsum.tourTrainCost(o, d);
        if(tourTrainCost == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 332 && tourTrainCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 476 && tourTrainCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourTrainCost * (tourTrainCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 312 && tourTrainCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 312 && tourTrainCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 436 && tourTrainCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourTrainCost * (tourTrainCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
    }

    public double stopFreqUExp(int o, int d, int td, int tps, ModeChoice mc, TripType type, int toy, int numOfStops, boolean isOutBound) {
        double dist = o == d ? 0 : businessCarMap.get(getKey(o, d))[3];
        if(numOfStops == 0) {
            return exp(0.0);
        }
        else {
            return exp(stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_dist" + numOfStops) * dist
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_dura" + numOfStops) * td
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_party" + numOfStops) * tps
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_car" + numOfStops) * (mc == ModeChoice.CAR ? 1 : 0)
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_busi" + numOfStops) * (type == TripType.BUSINESS ? 1 : 0)
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_plea" + numOfStops) * (type == TripType.PLEASURE ? 1 : 0)
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart2_" + numOfStops) * (toy == 2 ? 1 : 0)
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart3_" + numOfStops) * (toy == 3 ? 1 : 0)
                       + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart4_" + numOfStops) * (toy == 4 ? 1 : 0)
                       + stopFreqCoefs.get("cons" + (isOutBound ? "o" : "i") + numOfStops));
        }
    }

    public double stopTypeUExp(int numOfStop, TripType type, int tps, ModeChoice mc, boolean isOutBound) {
        if(type == TripType.PLEASURE) {
            return exp(0.0);
        }
        else {
            return exp(stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_sec_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 2 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_thir_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 3 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_fou_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 4 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_pt_" + (type == TripType.BUSINESS ? "b" : "pb")) * (type == TripType.PLEASURE ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_pbt_" + (type == TripType.BUSINESS ? "b" : "pb")) * (type == TripType.PERSONAL_BUSINESS ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_party_" + (type == TripType.BUSINESS ? "b" : "pb")) * tps
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_car_" + (type == TripType.BUSINESS ? "b" : "pb")) * (mc == ModeChoice.CAR ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_air_" + (type == TripType.BUSINESS ? "b" : "pb")) * (mc == ModeChoice.AIR ? 1 : 0)
                   + stopTypeCoefs.get("con" + (isOutBound ? "o" : "i") + "s_" + (type == TripType.BUSINESS ? "b" : "pb")));

        }
    }

    public double stopLocUExp(Person2010 p, int so, int o, int d, int s, ModeChoice mc, TripType type, int toy, boolean isOutBound) {
        if(s == so || s == d || s == o) {
            return 0.0;
        }
        double gtc = generailizedTravelCost(p, so, o, d, s, mc, type, toy);
        sLog.debug("      gtc[" + s + "]: " + gtc);
        double dist = businessCarMap.get(getKey(so, d))[3];
        if(gtc == Double.NEGATIVE_INFINITY) {
            // meaning some key pair couldn't be found in train/car/air files.
            return 0.0;
        }
        return exp(stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_det_dist") * detDist(so, d, s)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_s") * (dist < 150 ? gtc : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_m") * (dist >= 150 && dist < 550 ? gtc : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_l") * (dist >= 550 ? gtc : 0)
                   + (isOutBound ? stopLocCoefs.get("po_gtc_lc") : stopLocCoefs.get("pi_gtc_mc"))
                     * (isOutBound ? (dist >= 550 ? gtc : 0) : (dist >= 150 && dist < 550 ? gtc : 0)) * (mc == ModeChoice.CAR ? 1 : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_emp") * msaEmpMap.get(s)[0]
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_hh") * msaEmpMap.get(s)[1]
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_nmsa") * (1 - zonIdMap.get(idMsaMap.get(s))[1]));
    }

    private double generailizedTravelCost(Person2010 p, int so, int o, int d, int s, ModeChoice mc, TripType type, int toy) {
        return detourTravelCost(p, so, d, s, mc, type, toy) + detourTravelTime(p, so, d, s, mc, type) * vot(p, o, d, mc, type, toy);
    }

    private double detourTravelCost(Person2010 p, int so, int d, int s, ModeChoice mc, TripType type, int toy) {
        if(mc == ModeChoice.CAR) {
            return (logsum.tourCarCost(p.getIncLevel(), so, s, type)
                    + logsum.tourCarCost(p.getIncLevel(), s, d, type)
                    - logsum.tourCarCost(p.getIncLevel(), so, d, type)) / 2;
        }
        else if(mc == ModeChoice.AIR) {
            return (logsum.tourAirCost(so, s, toy)
                    + logsum.tourAirCost(s, d, toy)
                    - logsum.tourAirCost(so, d, toy)) / 2;
        }
        else {
            // mc == ModeChoice.TRAIN
            if((logsum.tourTrainCost(so, s) == Double.NEGATIVE_INFINITY
                || logsum.tourTrainCost(s, d) == Double.NEGATIVE_INFINITY)
               && logsum.tourTrainCost(so, d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            return (logsum.tourTrainCost(so, s) + logsum.tourTrainCost(s, d) - logsum.tourTrainCost(so, d)) / 2;
        }
    }

    private double detourTravelTime(Person2010 p, int so, int d, int s, ModeChoice mc, TripType type) {
        if(mc == ModeChoice.CAR) {
            return (logsum.tourCarTime(so, s, type)
                    + logsum.tourCarTime(s, d, type)
                    - logsum.tourCarTime(so, d, type)) / 2;
        }
        else if(mc == ModeChoice.AIR) {
            return (logsum.tourAirTime(so, s)
                    + logsum.tourAirTime(s, d)
                    - logsum.tourAirTime(s, d)) / 2;
        }
        else {
            // mc == ModeChoice.TRAIN
            if((logsum.tourTrainTime(so, s) == Double.NEGATIVE_INFINITY
                || logsum.tourTrainTime(s, d) == Double.NEGATIVE_INFINITY)
               && logsum.tourTrainTime(so, d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            return (logsum.tourTrainTime(so, s) + logsum.tourTrainTime(s, d) - logsum.tourTrainTime(so, d)) / 2;
        }
    }

    private double vot(Person2010 p, int o, int d, ModeChoice mc, TripType type, int toy) {
        if(mc == ModeChoice.CAR) {
            double tcc = logsum.tourCarCost(p.getIncLevel(), o, d, type);
            return getVotValue(type, tcc);
        }
        else if(mc == ModeChoice.AIR) {
            double tac = logsum.tourAirCost(o, d, toy);
            return getVotValue(type, tac);
        }
        else {
            // mc == ModeChoice.TRAIN
            double ttc = logsum.tourTrainCost(o, d);
            return getVotValue(type, ttc);
        }

    }

    private double getVotValue(TripType type, double cost) {
        if(type == TripType.BUSINESS) {
            if(cost <= 188) {
                return 1.095384615;
            }
            if(cost > 188 && cost <= 332) {
                return 3.811563169;
            }
            if(cost > 332 && cost <= 476) {
                return 5.377643505;
            }
            if(cost > 476 && cost <= 620) {
                return 9.621621622;
            }
            if(cost > 620) {
                return 12.8057554;
            }
            return Double.NEGATIVE_INFINITY;
        }
        if(type == TripType.PLEASURE) {
            if(cost <= 188) {
                return 6.230200634;
            }
            if(cost > 188 && cost <= 312) {
                return 13.59447005;
            }
            if(cost > 312 && cost <= 436) {
                return 65.55555556;
            }
            if(cost > 436) {
                return 176.119403;
            }
            return Double.NEGATIVE_INFINITY;
        }
        if(type == TripType.PERSONAL_BUSINESS) {
            if(cost <= 188) {
                return 2.582677165;
            }
            if(cost > 188 && cost <= 312) {
                return 5.754385965;
            }
            if(cost > 312 && cost <= 436) {
                return 8.282828283;
            }
            if(cost > 436 && cost <= 560) {
                return 11.88405797;
            }
            if(cost > 560) {
                return 30.37037037;
            }
            return Double.NEGATIVE_INFINITY;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private double detDist(int so, int d, int s) {
        if(businessCarMap.get(getKey(so, s))[3] < 50) {
            return Double.POSITIVE_INFINITY;
        }
        if(so == s) {
            return Double.POSITIVE_INFINITY;
        }
        return businessCarMap.get(getKey(so, s))[3] + businessCarMap.get(getKey(s, d))[3] - businessCarMap.get(getKey(so, d))[3];
    }

    private void preCalculateLogsum() {
        sLog.info("Started pre-calculate logsum.");
        Person2010 p = new Person2010();
        for (int o = 1; o <= Math.alt; o++) {
            sLog.debug("  " + o + " out of " + Math.alt + " done.");
            for (int d = 1; d <= Math.alt; d++) {
                if(o == d) {
                    continue;
                }
                try {
                    p.setIncLevel(1);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));

                    p.setIncLevel(2);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));

                    p.setIncLevel(3);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));
                }
                catch (InvalidValueException e) {
                    sLog.error(e.getLocalizedMessage(), e);
                    System.exit(1);
                }
            }
        }
        sLog.info("Completed. quarter0 size: " + logsumCacheQ0.size()
                  + ", quarter1 size: " + logsumCacheQ1.size()
                  + ", quarter2 size: " + logsumCacheQ2.size()
                  + ", quarter3 size: " + logsumCacheQ3.size()
                  + ", quarter4 size: " + logsumCacheQ4.size());
    }

    private Double getLogsum(Person2010 p, int o, int d, TripType type, int quarter) {
        double logsum = Double.NEGATIVE_INFINITY;
        if(quarter == INVALID_QUARTER) {
            logsum = logsumCacheQ0.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 1) {
            logsum = logsumCacheQ1.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 2) {
            logsum = logsumCacheQ2.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 3) {
            logsum = logsumCacheQ3.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 4) {
            logsum = logsumCacheQ4.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        return logsum;
    }

    public Integer MonteCarloMethod(List<Double> pList, Map<Double, Integer> pMap, double smpl) {
        sLog.debug("    Monte carlo rand: " + smpl);
        Collections.sort(pList);
        double tmpSum = 0.0;
        int pickedIndex = - 1;
        for (int ptr = 0; ptr < pList.size(); ptr++) {
            double tmp = tmpSum;
            tmpSum += pList.get(ptr);
            if(smpl >= tmp && smpl < tmpSum) {
                pickedIndex = ptr;
                break;
            }
        }
        if(pickedIndex == -1) {
            pickedIndex = pList.size() - 1;
        }
        return pMap.get(pList.get(pickedIndex));
    }

    /**
     * File Loading Methods below
     * @return 
     */
    private HashMap<String, Double[]> initOtherCarMap() {
        sLog.info("Initialize otherCarMap.");
        HashMap<String, Double[]> ocm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_car_other"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    ocm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return ocm;
    }

    private HashMap<String, Double[]> initBusinessCarMap() {
        sLog.info("Initialize businessCarMap.");
        HashMap<String, Double[]> bcm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_car_business"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    bcm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return bcm;
    }

    private HashMap<String, Double[]> initAirMap() {
        sLog.info("Initialize airMap.");
        HashMap<String, Double[]> am = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.air_skim_avg"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O_MSA")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String airTime = ExcelUtils.getColumnValue(7, line);
                    String airCost = ExcelUtils.getColumnValue(8, line);
                    Double[] value = {
                        Double.parseDouble(airTime),
                        Double.parseDouble(airCost)
                    };
                    am.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return am;
    }

    private HashMap<String, Double[]> initTrainMap() {
        sLog.info("Initialize trainMap.");
        HashMap<String, Double[]> tm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_train"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("A_MSA")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String trainTime = ExcelUtils.getColumnValue(5, line);
                    String trainCost = ExcelUtils.getColumnValue(6, line);
                    Double[] value = {
                        Double.parseDouble(trainTime),
                        Double.parseDouble(trainCost)
                    };
                    tm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return tm;
    }

    private HashMap<Integer, Double[]> initMsaEmpMap() {
        sLog.info("Initialize msaEmpMap.");
        HashMap<Integer, Double[]> mem = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msa_emp"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA")) {
                    Integer key = zonIdMap.get(Integer.parseInt(ExcelUtils.getColumnValue(1, line)))[0];
                    Double emp = Double.parseDouble(ExcelUtils.getColumnValue(3, line));
                    Double hh = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    Double[] value = {emp, hh};
                    mem.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return mem;
    }

    private HashMap<String, Double[]> initQuarterAirMap() {
        sLog.info("Initialize quarterAirMap.");
        HashMap<String, Double[]> qam = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_1"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);
                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[1] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[1] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_2"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[2] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[2] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_3"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[3] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[3] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_4"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[4] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[4] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        return qam;
    }

    private HashMap<Integer, Integer[]> initZoneIdMapAndIdMsaMap() {
        sLog.info("Initialize zone id.");
        HashMap<Integer, Integer[]> zone = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.zoneid"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA/NMSA")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer[] value = {Integer.parseInt(ExcelUtils.getColumnValue(2, line)), Integer.parseInt(ExcelUtils.getColumnValue(3, line))};
                    zone.put(key, value);
                    idMsaMap.put(value[0], key);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return zone;
    }

    private String getKey(int o, int d) {
        return Integer.toString(o) + "-" + Integer.toString(d);
    }
}
