package com.infra.alert;

import android.content.Context;
import android.telephony.SmsManager;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SmsHttpServer extends NanoHTTPD {
    private final Context ctx;
    private final DatabaseHelper db;
    private static final String ADMIN_PASS = "admin123";

    public SmsHttpServer(Context ctx, int port) throws java.io.IOException {
        super(port);
        this.ctx = ctx;
        this.db = new DatabaseHelper(ctx);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> body = new HashMap<>();
        if (Method.POST.equals(method)) {
            try { session.parseBody(body); } catch (Exception e) { e.printStackTrace(); }
        }
        Map<String, String> params = session.getParms();
        String postData = body.getOrDefault("postData", "{}");

        try {
            if (uri.equals("/") || uri.equals("/operator"))
                return html(operatorPage());
            if (uri.equals("/admin")) {
                String pass = params.getOrDefault("pass", "");
                return html(pass.equals(ADMIN_PASS) ? adminPage() : loginPage());
            }
            if (uri.equals("/admin/login") && Method.POST.equals(method)) {
                JSONObject j = new JSONObject(postData);
                return json(j.optString("pass").equals(ADMIN_PASS) ? "{\"success\":true}" : "{\"success\":false}");
            }
            if (uri.equals("/admin/add-downtime") && Method.POST.equals(method)) {
                JSONObject j = new JSONObject(postData);
                db.addDowntimeType(j.optString("name"), j.optString("message"));
                return json("{\"success\":true}");
            }
            if (uri.equals("/admin/add-engineer") && Method.POST.equals(method)) {
                JSONObject j = new JSONObject(postData);
                db.addEngineer(j.optString("name"), j.optString("phone"), j.optString("downtime_id"));
                return json("{\"success\":true}");
            }
            if (uri.equals("/admin/delete-downtime") && Method.POST.equals(method)) {
                db.deleteDowntimeType(new JSONObject(postData).optInt("id"));
                return json("{\"success\":true}");
            }
            if (uri.equals("/admin/delete-engineer") && Method.POST.equals(method)) {
                db.deleteEngineer(new JSONObject(postData).optInt("id"));
                return json("{\"success\":true}");
            }
            if (uri.equals("/api/downtimes"))
                return json(db.getDowntimesJson());
            if (uri.equals("/api/engineers"))
                return json(db.getEngineersJson(params.getOrDefault("downtime_id", "")));
            if (uri.equals("/api/logs"))
                return json(db.getLogsJson());
            if (uri.equals("/api/send-sms") && Method.POST.equals(method)) {
                JSONObject j = new JSONObject(postData);
                String phone = j.optString("phone");
                String message = j.optString("message");
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phone, null, message, null, null);
                db.addLog(j.optString("engineer"), phone, j.optString("downtime"), message);
                return json("{\"success\":true,\"msg\":\"SMS sent!\"}");
            }
        } catch (Exception e) {
            return json("{\"success\":false,\"msg\":\"" + e.getMessage() + "\"}");
        }
        return html("<h2>404 Not Found</h2>");
    }

    private Response html(String content) {
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", content);
    }
    private Response json(String content) {
        Response r = newFixedLengthResponse(Response.Status.OK, "application/json", content);
        r.addHeader("Access-Control-Allow-Origin", "*");
        return r;
    }

    private String loginPage() {
        return "<!DOCTYPE html><html><head><title>Admin Login</title>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{box-sizing:border-box}body{font-family:Arial;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background:#1a1a2e}"
            + ".box{background:white;padding:40px;border-radius:12px;text-align:center;width:320px}"
            + "input,button{width:100%;padding:12px;margin:8px 0;border-radius:6px;border:1px solid #ddd;font-size:16px}"
            + "button{background:#e74c3c;color:white;border:none;cursor:pointer;font-weight:bold}</style></head>"
            + "<body><div class='box'><h2>&#128274; Admin Login</h2>"
            + "<input type='password' id='p' placeholder='Password'>"
            + "<button onclick='login()'>Login</button>"
            + "<p id='e' style='color:red'></p></div>"
            + "<script>function login(){fetch('/admin/login',{method:'POST',headers:{'Content-Type':'application/json'},"
            + "body:JSON.stringify({pass:document.getElementById('p').value})})"
            + ".then(r=>r.json()).then(d=>{if(d.success)window.location='/admin?pass='+document.getElementById('p').value;"
            + "else document.getElementById('e').innerText='Wrong password!'})}"
            + "document.getElementById('p').addEventListener('keypress',e=>{if(e.key==='Enter')login()})"
            + "</script></body></html>";
    }

    private String adminPage() {
        return "<!DOCTYPE html><html><head><title>Admin - Infra Alert</title>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{box-sizing:border-box}body{font-family:Arial;margin:0;background:#f0f2f5}"
            + ".hdr{background:#2c3e50;color:white;padding:15px 20px;display:flex;justify-content:space-between;align-items:center}"
            + ".hdr a{color:#f39c12;text-decoration:none}.con{max-width:850px;margin:20px auto;padding:0 15px}"
            + ".card{background:white;border-radius:10px;padding:20px;margin:15px 0;box-shadow:0 2px 8px rgba(0,0,0,.1)}"
            + "input,select,textarea{width:100%;padding:10px;margin:6px 0;border:1px solid #ddd;border-radius:5px;font-size:14px}"
            + ".btn{padding:10px 18px;border:none;border-radius:5px;cursor:pointer;font-size:14px;font-weight:bold}"
            + ".bp{background:#3498db;color:white}.bd{background:#e74c3c;color:white}.bs{background:#2ecc71;color:white}"
            + ".tabs{display:flex;gap:8px;margin:15px 0;flex-wrap:wrap}"
            + ".tab{padding:10px 18px;cursor:pointer;border-radius:6px;background:#ddd;font-weight:bold}"
            + ".tab.active{background:#2c3e50;color:white}"
            + "table{width:100%;border-collapse:collapse}th,td{padding:10px;text-align:left;border-bottom:1px solid #eee}"
            + "th{background:#f8f9fa;font-weight:bold}.ok{background:#d4edda;color:#155724;padding:10px;border-radius:5px;margin:8px 0}"
            + ".row{display:flex;gap:10px}.row>*{flex:1}</style></head><body>"
            + "<div class='hdr'><h2 style='margin:0'>&#9881; Admin Panel</h2><a href='/'>&#128226; Operator View</a></div>"
            + "<div class='con'>"
            + "<div class='tabs'>"
            + "<div class='tab active' onclick='showTab(\"dt\",this)'>&#128203; Downtime Types</div>"
            + "<div class='tab' onclick='showTab(\"eng\",this)'>&#128119; Engineers</div>"
            + "<div class='tab' onclick='showTab(\"log\",this)'>&#128196; SMS Logs</div>"
            + "</div>"

            // Downtime Tab
            + "<div id='dt'>"
            + "<div class='card'><h3>&#10133; Add Downtime Type</h3>"
            + "<input id='dn' placeholder='Downtime name (e.g. Network Down)'>"
            + "<textarea id='dm' rows='2' placeholder='Default SMS message for this downtime type'></textarea>"
            + "<button class='btn bp' onclick='addDT()'>Add Downtime Type</button>"
            + "<div id='dtst'></div></div>"
            + "<div class='card'><h3>&#128203; Downtime Types List</h3><div id='dtlist'>Loading...</div></div></div>"

            // Engineer Tab
            + "<div id='eng' style='display:none'>"
            + "<div class='card'><h3>&#10133; Add Engineer</h3>"
            + "<div class='row'><input id='en' placeholder='Engineer Name'><input id='ep' placeholder='Phone +91XXXXXXXXXX'></div>"
            + "<select id='edt'><option value=''>Select Downtime Type</option></select>"
            + "<button class='btn bp' onclick='addEng()'>Add Engineer</button>"
            + "<div id='engst'></div></div>"
            + "<div class='card'><h3>&#128119; Engineers List</h3><div id='englist'>Loading...</div></div></div>"

            // Logs Tab
            + "<div id='log' style='display:none'>"
            + "<div class='card'><h3>&#128196; SMS Sent Logs</h3><div id='loglist'>Loading...</div></div></div>"
            + "</div>"

            + "<script>"
            + "function showTab(t,el){['dt','eng','log'].forEach(x=>document.getElementById(x).style.display='none');"
            + "document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));"
            + "document.getElementById(t).style.display='block';el.classList.add('active');"
            + "if(t==='dt')loadDT();if(t==='eng'){loadDT();loadEng();}if(t==='log')loadLog();}"
            + "function post(u,d){return fetch(u,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(d)}).then(r=>r.json())}"
            + "function addDT(){const n=document.getElementById('dn').value,m=document.getElementById('dm').value;"
            + "if(!n||!m){alert('Fill all fields');return;}"
            + "post('/admin/add-downtime',{name:n,message:m}).then(()=>{document.getElementById('dtst').innerHTML='<div class=\"ok\">&#10003; Added!</div>';"
            + "document.getElementById('dn').value='';document.getElementById('dm').value='';loadDT();})}"
            + "function addEng(){const n=document.getElementById('en').value,p=document.getElementById('ep').value,d=document.getElementById('edt').value;"
            + "if(!n||!p||!d){alert('Fill all fields');return;}"
            + "post('/admin/add-engineer',{name:n,phone:p,downtime_id:d}).then(()=>{document.getElementById('engst').innerHTML='<div class=\"ok\">&#10003; Added!</div>';"
            + "document.getElementById('en').value='';document.getElementById('ep').value='';loadEng();})}"
            + "function loadDT(){fetch('/api/downtimes').then(r=>r.json()).then(data=>{"
            + "let h='<table><tr><th>Name</th><th>Default Message</th><th>Action</th></tr>';"
            + "data.forEach(d=>h+=`<tr><td><b>${d.name}</b></td><td>${d.message}</td><td><button class=\"btn bd\" onclick=\"delDT(${d.id})\">Delete</button></td></tr>`);"
            + "document.getElementById('dtlist').innerHTML=h+'</table>';"
            + "const sel=document.getElementById('edt');if(sel){sel.innerHTML='<option value=\"\">Select Downtime Type</option>';"
            + "data.forEach(d=>sel.innerHTML+=`<option value=\"${d.id}\">${d.name}</option>`);} })}"
            + "function loadEng(){fetch('/api/engineers').then(r=>r.json()).then(data=>{"
            + "let h='<table><tr><th>Name</th><th>Phone</th><th>Downtime Type</th><th>Action</th></tr>';"
            + "data.forEach(d=>h+=`<tr><td><b>${d.name}</b></td><td>${d.phone}</td><td>${d.downtime_name}</td><td><button class=\"btn bd\" onclick=\"delEng(${d.id})\">Delete</button></td></tr>`);"
            + "document.getElementById('englist').innerHTML=h+'</table>';})}"
            + "function loadLog(){fetch('/api/logs').then(r=>r.json()).then(data=>{"
            + "let h='<table><tr><th>Time</th><th>Engineer</th><th>Phone</th><th>Downtime</th><th>Message</th></tr>';"
            + "data.forEach(d=>h+=`<tr><td>${d.time}</td><td>${d.engineer}</td><td>${d.phone}</td><td>${d.downtime}</td><td>${d.message}</td></tr>`);"
            + "document.getElementById('loglist').innerHTML=h+'</table>';})}"
            + "function delDT(id){if(confirm('Delete this downtime type?'))post('/admin/delete-downtime',{id}).then(loadDT)}"
            + "function delEng(id){if(confirm('Delete this engineer?'))post('/admin/delete-engineer',{id}).then(loadEng)}"
            + "loadDT();"
            + "</script></body></html>";
    }

    private String operatorPage() {
        return "<!DOCTYPE html><html><head><title>Infra Alert</title>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{box-sizing:border-box}body{font-family:Arial;margin:0;background:#1a1a2e;min-height:100vh}"
            + ".hdr{background:#e74c3c;color:white;padding:15px 20px;display:flex;justify-content:space-between;align-items:center}"
            + ".hdr a{color:white;text-decoration:none;font-size:13px;background:rgba(0,0,0,.2);padding:6px 12px;border-radius:5px}"
            + ".con{max-width:500px;margin:30px auto;padding:0 15px}"
            + ".card{background:white;border-radius:12px;padding:25px;box-shadow:0 5px 20px rgba(0,0,0,.3)}"
            + "label{font-weight:bold;color:#2c3e50;display:block;margin-top:12px}"
            + "select,textarea{width:100%;padding:12px;margin:5px 0;border:1px solid #ddd;border-radius:6px;font-size:15px}"
            + ".send{width:100%;padding:15px;background:#e74c3c;color:white;border:none;border-radius:6px;font-size:18px;cursor:pointer;font-weight:bold;margin-top:15px}"
            + ".send:active{background:#c0392b}"
            + ".info{background:#eaf0fb;border-radius:6px;padding:10px;margin:6px 0;font-size:14px}"
            + ".ok{background:#d4edda;color:#155724;padding:12px;border-radius:6px;margin:10px 0;text-align:center;font-size:16px}"
            + ".err{background:#f8d7da;color:#721c24;padding:12px;border-radius:6px;margin:10px 0;text-align:center}</style></head>"
            + "<body><div class='hdr'><h2 style='margin:0'>&#128680; Infra Alert</h2>"
            + "<a href='/admin'>&#9881; Admin</a></div>"
            + "<div class='con'><div class='card'>"
            + "<h3 style='color:#e74c3c;margin-top:0'>Send Downtime Alert</h3>"
            + "<label>Downtime Type:</label>"
            + "<select id='dt' onchange='loadEng()'><option value=''>-- Select Downtime --</option></select>"
            + "<label>Assign Engineer:</label>"
            + "<select id='eng' onchange='showInfo()'><option value=''>-- Select Engineer --</option></select>"
            + "<div id='info' class='info' style='display:none'></div>"
            + "<label>Message:</label>"
            + "<textarea id='msg' rows='4' placeholder='Alert message...'></textarea>"
            + "<button class='send' onclick='sendSMS()'>&#128241; Send SMS Alert</button>"
            + "<div id='st'></div></div></div>"
            + "<script>let engs=[];"
            + "fetch('/api/downtimes').then(r=>r.json()).then(data=>{"
            + "const s=document.getElementById('dt');"
            + "data.forEach(d=>s.innerHTML+=`<option value='${d.id}' data-m='${d.message}'>${d.name}</option>`)})"
            + "function loadEng(){const s=document.getElementById('dt'),o=s.options[s.selectedIndex];"
            + "document.getElementById('msg').value=o?o.getAttribute('data-m'):'';"
            + "document.getElementById('info').style.display='none';"
            + "if(!s.value)return;"
            + "fetch('/api/engineers?downtime_id='+s.value).then(r=>r.json()).then(data=>{engs=data;"
            + "const e=document.getElementById('eng');e.innerHTML='<option value=\"\">-- Select Engineer --</option>';"
            + "data.forEach(x=>e.innerHTML+=`<option value='${x.id}'>${x.name}</option>`);})}"
            + "function showInfo(){const id=document.getElementById('eng').value,e=engs.find(x=>x.id==id),d=document.getElementById('info');"
            + "if(e){d.style.display='block';d.innerHTML=`&#128119; <b>${e.name}</b> &nbsp;|&nbsp; &#128222; ${e.phone}`;}else d.style.display='none';}"
            + "function sendSMS(){const dt=document.getElementById('dt'),engId=document.getElementById('eng').value,"
            + "msg=document.getElementById('msg').value,eng=engs.find(x=>x.id==engId),"
            + "dtName=dt.options[dt.selectedIndex]?dt.options[dt.selectedIndex].text:'';"
            + "if(!engId||!msg){document.getElementById('st').innerHTML='<div class=\"err\">&#10060; Select engineer and enter message!</div>';return;}"
            + "document.getElementById('st').innerHTML='<div class=\"info\">&#8987; Sending...</div>';"
            + "fetch('/api/send-sms',{method:'POST',headers:{'Content-Type':'application/json'},"
            + "body:JSON.stringify({phone:eng.phone,message:msg,engineer:eng.name,downtime:dtName})})"
            + ".then(r=>r.json()).then(d=>{document.getElementById('st').innerHTML=d.success?"
            + "`<div class='ok'>&#10003; SMS sent to ${eng.name}!</div>`:"
            + "`<div class='err'>&#10060; ${d.msg}</div>`;})}"
            + "</script></body></html>";
    }
}
