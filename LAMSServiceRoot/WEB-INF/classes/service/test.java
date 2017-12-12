package service;

import java.util.*;
import business.*;

public class test {

   public static void main(String[] args){
      // create service
      LAMSService service = new LAMSService();

      // init
      // System.out.println(service.initialize());
      service.initialize();

      // get one appointment
      // System.out.println("Appointment 700:");
      System.out.println(service.getAppointment("700"));

      // all appointments
      // System.out.println("All Appointments:");
      // System.out.println(service.getAllAppointments());

      // add appointment that does not overlap
      // String response = service.addAppointment("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?><appointment><date>2017-02-02</date><time>10:00</time><patientId>220</patientId><physicianId>20</physicianId><pscId>520</pscId><phlebotomistId>110</phlebotomistId><labTests><test id=\"86900\" dxcode=\"292.9\" /><test id=\"86609\" dxcode=\"307.3\" /></labTests></appointment>");
      // System.out.println(response);
      // add appointment that does overlap
      // String response2 = service.addAppointment("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?><appointment><date>2017-02-02</date><time>10:00</time><patientId>220</patientId><physicianId>20</physicianId><pscId>520</pscId><phlebotomistId>110</phlebotomistId><labTests><test id=\"86900\" dxcode=\"292.9\" /><test id=\"86609\" dxcode=\"307.3\" /></labTests></appointment>");
      // System.out.println(response2);

      // test business layer
      // BusinessLayer.isOverlapping(java.sql.Time.valueOf("12:00:00"),java.sql.Time.valueOf("01:00:00"));
   }
}
