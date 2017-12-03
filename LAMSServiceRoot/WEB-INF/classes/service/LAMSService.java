package service;

import javax.jws.*;
import business.*;
import components.data.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.InputSource;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("LAMSService")
public class LAMSService {

    @Context
    private UriInfo context;
    private DBSingleton dbSingleton;
    private String uri;
    private final String xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";

    /**
    * Initialize the database.
    */
    // @GET
    // @Produces("application/xml")
    public String initialize(){
        dbSingleton = DBSingleton.getInstance();
        dbSingleton.db.initialLoad("LAMS");
        this.uri = this.context.getBaseUri().toString();
        return this.uri+"application.wadl";
    }

    /**
    * Returns an xml string containing all appointments.
    */
    @Path("/Appointments")
    @GET
    @Consumes({"text/plain","text/html"})
    @Produces("application/xml")
    public String getAllAppointments(){
        String xml = xmlHead + "<AppointmentList>";

        dbSingleton = DBSingleton.getInstance();
        System.out.println("All appointments");
        List<Object> objs = dbSingleton.db.getData("Appointment", "");
        for (Object obj : objs){
            xml += formatAppointment(obj);
        }
        xml += "<uri>"+uri+"</uri>";
        xml += "</AppointmentList>";

        return xml;
    }

    /**
    * Get the id of a new appointment to add.
    */
    private String getNewAppointmentId(){
        int newId = -1;

        dbSingleton = DBSingleton.getInstance();
        List<Object> objs = dbSingleton.db.getData("Appointment", "");
        for (Object obj : objs){
            int id = Integer.parseInt(((Appointment)obj).getId());
            if(id > newId){
                newId = id;
            }
        }
        return Integer.toString(++newId);
    }

    /**
    * Returns an xml string of appointment from id.
    * @param appointmentID The id of the appointment to get.
    */
    @Path("/Appointments/{appointmentID}")
    @GET
    @Consumes({"text/plain","text/html"})
    @Produces("application/xml")
    public String getAppointment(@PathParam("appointmentID") String appointmentID){
        String xml = xmlHead + "<AppointmentList>";

        dbSingleton = DBSingleton.getInstance();
        List<Object> objs = dbSingleton.db.getData("Appointment", "id='"+appointmentID+"'");
        for (Object obj : objs){
            xml += formatAppointment(obj);
        }

        xml += "</AppointmentList>";
        return xml;
    }

    /**
    * Takes in Appointment and creates xml string from it.
    * @param obj The appointment object.
    */
    private String formatAppointment(Object obj) {
        String date = ((Appointment)obj).getApptdate().toString();
        String id = ((Appointment)obj).getId();
        String time = ((Appointment)obj).getAppttime().toString();
        String xml = "<appointment date=\""+date+"\" id=\""+id+"\" time=\""+time+"\">";

        Patient patient = ((Appointment)obj).getPatientid();
        xml += tag("patient", patient.getId(), "id");
        xml += tag("name", patient.getName());
        xml += tag("address", patient.getAddress());
        xml += tag("insurance", Character.toString(patient.getInsurance()));
        xml += tag("dob", patient.getDateofbirth().toString());
        xml += "</patient>";

        Phlebotomist phleb = ((Appointment)obj).getPhlebid();
        xml += tag("phlebotomist", phleb.getId(), "id");
        xml += tag("name", phleb.getName());
        xml += "</phlebotomist>";

        PSC psc = ((Appointment)obj).getPscid();
        xml += tag("psc", psc.getId(), "id");
        xml += tag("name", psc.getName());
        xml += "</psc>";

        // lab tests
        List<AppointmentLabTest> tests = ((Appointment)obj).getAppointmentLabTestCollection();
        LabTest test = null;
        Diagnosis dx = null;
        xml += "<allLabTests>";
        for (AppointmentLabTest apptTest : tests) {
            test = apptTest.getLabTest();
            dx = apptTest.getDiagnosis();
            xml += "<appointmentLabTest appointmentId=\""+id+"\" dxcode=\""+dx.getCode()+"\" labTestId=\""+test.getKey()+"\"/>";
        }

        xml += "</allLabTests></appointment>";
        return xml;
    }

    /**
    * Create an xml tag.
    * @param name The tag name.
    * @param value The text value of the element.
    */
    private static String tag(String name, String value) {
        return String.format("<%s>%s</%s>\n", name, value, name);
    }

    /**
    * Create an xml element with given tag name and with text value.
    * @param name The tag name.
    * @param value The value of the attribute of the xml element.
    * @param propertyName The name of the attribute of the xml element.
    */
    private static String tag(String name, String value, String propertyName) {
        return String.format("<%s %s=\"%s\">\n", name, propertyName, value);
    }

    /**
    * Create an appointment from xml string.
    * @param xml The string containing appointment info.
    */
    @Path("/Appointments")
    @PUT
    @Consumes({"text/xml","application/xml"})
    @Produces("application/xml")
    public String addAppointment(@QueryParam("xml") String xml){
        String response = "";
        String date, time, patientId, physicianId, pscId, newApptId, phlebotomistId;
        date = time = patientId = physicianId = pscId = phlebotomistId = "";
        List<AppointmentLabTest> tests = new ArrayList<AppointmentLabTest>();
        newApptId = getNewAppointmentId();

        // get values from xml
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
            NodeList nList = doc.getElementsByTagName("appointment");

            Node apptNode = nList.item(0); // there should be only one appointment

            if(apptNode.getNodeType() == Node.ELEMENT_NODE){
                Element eElement = (Element)apptNode;

                date = eElement.getElementsByTagName("date").item(0).getTextContent();
                time = eElement.getElementsByTagName("time").item(0).getTextContent();
                patientId = eElement.getElementsByTagName("patientId").item(0).getTextContent();
                physicianId = eElement.getElementsByTagName("physicianId").item(0).getTextContent();
                pscId = eElement.getElementsByTagName("pscId").item(0).getTextContent();
                phlebotomistId = eElement.getElementsByTagName("phlebotomistId").item(0).getTextContent();

                // get lab tests
                NodeList testsNode = eElement.getElementsByTagName("test");
                for(int i = 0 ; i < testsNode.getLength() ; i++){
                    String testId = "";
                    String dxcode = "";
                    Element e = (Element)testsNode.item(i);
                    testId = e.getAttribute("id");
                    dxcode = e.getAttribute("dxcode");

                    // create a test & add to list
                    AppointmentLabTest test = new AppointmentLabTest(newApptId,testId,dxcode);
                    test.setDiagnosis((Diagnosis)dbSingleton.db.getData("Diagnosis", "code='"+dxcode+"'").get(0));
                    test.setLabTest((LabTest)dbSingleton.db.getData("LabTest","id='"+testId+"'").get(0));
                    tests.add(test);
                }
            }
        } catch(ParserConfigurationException pce){
            System.out.println("ParserConfigurationException caught at addAppointment(String): ");
        } catch(Exception e){
            System.out.println("Exception caught at addAppointment(String): ");
            e.printStackTrace();
        }

        // create appointment
        Appointment newAppt = new Appointment(newApptId,java.sql.Date.valueOf(date),java.sql.Time.valueOf(time+":00"));
        Patient patient = (Patient)dbSingleton.db.getData("Patient", "id='"+patientId+"'").get(0);
        Phlebotomist phleb = (Phlebotomist)dbSingleton.db.getData("Phlebotomist", "id='"+phlebotomistId+"'").get(0);
        PSC psc = (PSC)dbSingleton.db.getData("PSC", "id='"+pscId+"'").get(0);
        newAppt.setAppointmentLabTestCollection(tests);
        newAppt.setPatientid(patient);
        newAppt.setPhlebid(phleb);
        newAppt.setPscid(psc);

        // validate appointment
        // get all appointments on same day
        List<Object> sameDayAppts = dbSingleton.db.getData("Appointment", "DATE(apptdate)='"+date+"'");

        if(BusinessLayer.isAvailable(newAppt, sameDayAppts) && dbSingleton.db.addData(newAppt)){
            response = getAppointment(newApptId);
        } else {
            response = xmlHead+"<AppointmentList><error>ERROR:Appointment is not available</error></AppointmentList>";
        }

        return response;
    }

}
