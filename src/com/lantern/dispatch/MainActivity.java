package com.lantern.dispatch;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.graphics.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    Game game;Board board;boolean foreground;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(5894);game=new Game();restore();board=new Board();setContentView(board);}
    @Override public void onResume(){super.onResume();foreground=true;if(board!=null){board.last=0;board.postInvalidateOnAnimation();}}
    @Override public void onPause(){foreground=false;save();super.onPause();}
    void show(AlertDialog dialog){int speed=game.speed;game.speed=0;dialog.setOnDismissListener(d->{game.speed=speed;if(board!=null)board.last=0;save();});dialog.show();}
    void save(){try{
        JSONObject j=new JSONObject();j.put("version",1);j.put("cash",game.cash);j.put("revenue",game.revenue);j.put("expenses",game.expenses);j.put("rides",game.rides);j.put("missed",game.missed);j.put("elapsed",game.elapsed);j.put("rating",game.rating);j.put("speed",game.speed);j.put("auto",game.autoDispatch);j.put("milestone",game.milestone);j.put("sequence",game.spawnSequence);j.put("next",game.nextRide);j.put("debt",game.marginDebt);j.put("dividends",game.dividends);j.put("stage",game.historyStage);j.put("calls",game.marginCalls);j.put("marketTick",game.marketTick);j.put("model",game.businessModel);j.put("gross",game.grossFares);
        j.put("spawnClock",game.spawnClock);j.put("costClock",game.costClock);j.put("marketClock",game.marketClock);j.put("competitionClock",game.competitionClock);
        JSONArray ds=new JSONArray();for(Game.District d:game.districts)ds.put(d.open);j.put("districts",ds);
        JSONArray bs=new JSONArray();for(boolean b:game.branches)bs.put(b);j.put("branches",bs);
        JSONArray cs=new JSONArray();for(Game.Company c:game.companies){JSONArray h=new JSONArray();for(int v:c.priceHistory)h.put(v);cs.put(new JSONArray().put(c.price).put(c.owned).put(c.cash).put(c.served).put(c.merged).put(h));}j.put("companies",cs);
        JSONArray rs=new JSONArray();for(Game.Ride r:game.requests)rs.put(rideJson(r));j.put("requests",rs);
        JSONArray ts=new JSONArray();for(Game.Taxi t:game.taxis){JSONObject v=new JSONObject();v.put("id",t.id);v.put("cell",t.cell);v.put("status",t.status);v.put("at",t.at);v.put("progress",t.progress);v.put("charge",t.charge);v.put("ride",t.ride==null?JSONObject.NULL:rideJson(t.ride));JSONArray ps=new JSONArray();for(int p:t.path)ps.put(p);v.put("path",ps);ts.put(v);}j.put("taxis",ts);
        getPreferences(0).edit().putString("save",j.toString()).apply();
    }catch(JSONException e){game.message="Could not save. Try leaving the app again.";}}
    JSONObject rideJson(Game.Ride r)throws JSONException{return new JSONObject().put("id",r.id).put("from",r.from).put("to",r.to).put("fare",r.fare).put("created",r.created);}
    Game.Ride readRide(JSONObject j)throws JSONException{return new Game.Ride(j.getInt("id"),j.getInt("from"),j.getInt("to"),j.getLong("fare"),j.getDouble("created"));}
    void restore(){String text=getPreferences(0).getString("save",null);if(text==null)return;try{
        JSONObject j=new JSONObject(text);if(j.getInt("version")!=1)return;Game g=new Game();g.cash=j.getLong("cash");g.revenue=j.getLong("revenue");g.expenses=j.getLong("expenses");g.rides=j.getLong("rides");g.missed=j.getLong("missed");g.elapsed=j.getDouble("elapsed");g.rating=j.getDouble("rating");g.speed=j.getInt("speed");g.autoDispatch=j.getBoolean("auto");g.milestone=j.getBoolean("milestone");g.spawnSequence=j.getInt("sequence");g.nextRide=j.getInt("next");g.marginDebt=j.getLong("debt");g.dividends=j.getLong("dividends");g.historyStage=j.getInt("stage");g.marginCalls=j.getInt("calls");g.marketTick=j.getInt("marketTick");g.businessModel=j.getInt("model");g.grossFares=j.getLong("gross");
        g.spawnClock=j.optDouble("spawnClock",0);g.costClock=j.optDouble("costClock",0);g.marketClock=j.optDouble("marketClock",0);g.competitionClock=j.optDouble("competitionClock",0);
        JSONArray ds=j.getJSONArray("districts");for(int i=0;i<g.districts.size();i++)g.districts.get(i).open=ds.getBoolean(i);
        JSONArray bs=j.getJSONArray("branches");for(int i=0;i<g.branches.length;i++)g.branches[i]=bs.getBoolean(i);
        JSONArray cs=j.getJSONArray("companies");for(int i=0;i<g.companies.size();i++){JSONArray c=cs.getJSONArray(i);Game.Company v=g.companies.get(i);v.price=c.getInt(0);v.owned=c.getInt(1);v.cash=c.getLong(2);v.served=c.getLong(3);v.merged=c.getBoolean(4);if(c.length()>5){v.priceHistory.clear();JSONArray h=c.getJSONArray(5);for(int k=0;k<h.length();k++)v.priceHistory.add(h.getInt(k));}}
        g.requests.clear();JSONArray rs=j.getJSONArray("requests");for(int i=0;i<rs.length();i++)g.requests.add(readRide(rs.getJSONObject(i)));
        g.taxis.clear();JSONArray ts=j.getJSONArray("taxis");for(int i=0;i<ts.length();i++){JSONObject v=ts.getJSONObject(i);Game.Taxi t=new Game.Taxi(v.getInt("id"),v.getInt("cell"));t.status=v.getInt("status");t.at=v.getInt("at");t.progress=v.getDouble("progress");t.charge=v.getDouble("charge");if(!v.isNull("ride"))t.ride=readRide(v.getJSONObject("ride"));JSONArray ps=v.getJSONArray("path");t.path=new int[ps.length()];for(int k=0;k<t.path.length;k++)t.path[k]=ps.getInt(k);if(t.at<0||t.at>=t.path.length)throw new JSONException("Bad route");g.taxis.add(t);}
        g.message="Your mobility company is back in business.";game=g;
    }catch(Exception e){game.message="Previous save could not be restored. New company ready.";}}
    void help(){show(new AlertDialog.Builder(this).setTitle("Build a mobility empire")
        .setMessage("DISPATCH: tap a ride card to send the closest idle driver. Or enable auto dispatch.\n\nFLEET: recruit driver partners and choose a business model. Commission earns a share of each fare. Corporate commute unlocks in 2015. Driver subscription unlocks in 2022 and earns fixed quarterly fees instead of a fare commission.\n\nEXPAND: open Bengaluru districts, then launch regional branches after 20 rides and 3 districts.\n\nMARKET: buy fictional rival shares, borrow on margin, receive dividends and acquire companies. All exchange rules and prices are invented game mechanics.\n\nA year lasts 60 game seconds. HISTORY explains sourced events and their fictional gameplay effects. Progress saves locally. No real bookings, money or investments.")
        .setPositiveButton("Let's grow",null).create());}
    void company(int i){Game.Company c=game.companies.get(i);if(c.merged)return;
        String body=c.name+"\nPrice: "+Game.money(c.price)+" / share | own "+c.owned+"%\nFleet: "+c.fleet+" | rival cash: "+Game.money(c.cash)+"\nMargin debt: "+Game.money(game.marginDebt)+" | equity: "+Math.round(game.equityRatio()*100)+"%\nBorrowed purchases require 50% cash. Below 30% equity, the broker sells the full portfolio. Debt interest is 2% per quarter.\nMerger: own 60%, clear debt, then pay "+Game.money(game.mergerPrice(i))+" to integrate remaining shares, fleet, cash and a district.\nFictional exchange, not actual investment rules.";
        String[] actions={"Buy 10 shares · "+Game.money(c.price*10L),"Buy 10 on margin · "+Game.money(c.price*5L)+" cash","Sell 10 shares","Repay margin from cash","Complete merger · "+Game.money(game.mergerPrice(i))};
        show(new AlertDialog.Builder(this).setTitle(c.name+" · "+c.owned+"% owned").setItems(actions,(d,w)->{if(w==0)game.buyShares(i,false);if(w==1)game.buyShares(i,true);if(w==2)game.sellShares(i);if(w==3)game.repayMargin();if(w==4)game.merge(i);})
            .setNeutralButton("How this works",(d,w)->board.post(()->show(new AlertDialog.Builder(this).setTitle("Company ledger").setMessage(body).setPositiveButton("Understood",null).create())))
            .setNegativeButton("Close",null).create());}
    void regions(){String[] list=new String[6];for(int i=0;i<6;i++)list[i]=game.regions[i]+(game.branches[i]?" · operating":" · launch "+Game.money(game.branchPrice(i)));show(new AlertDialog.Builder(this).setTitle("National expansion · "+game.branchCount()+" / 6").setItems(list,(d,w)->{if(game.branches[w])game.message="This branch is already operating.";else game.expand(w);}).setNegativeButton("Close",null).create());}
    void models(){String[] options={"Commission · 32% per fare · steady demand","Corporate commute · 40% · peak-heavy (2015+)","Subscription · 80 cr/driver/quarter · no commission (2022+)"};show(new AlertDialog.Builder(this).setTitle("Choose your business model").setItems(options,(d,w)->game.setModel(w)).setNegativeButton("Cancel",null).create());}
    void history(){String story="REAL CONTEXT\n2013: international app-based cab services entered Bengaluru.\n2015: bike-taxi services and employee carpooling ventures emerged; the cab market also saw consolidation.\n2020: India announced its nationwide lockdown on 24 March.\n2022: a driver-direct auto-booking platform launched in Bengaluru in November.\n\nGAME INTERPRETATION\n2015 cuts onboarding costs 15% and unlocks corporate commute. In 2020 demand falls to 22% and invented stocks fall 52%. Driver subscriptions unlock in 2022. These percentages, valuations and strategic outcomes are invented.\n\nThe schematic Bengaluru map and all companies are fictional. National branches use real city names, but not real road data. No real company is represented by a rival. See the included historical notes for source links.";show(new AlertDialog.Builder(this).setTitle("Mobility chronicle · 2011–2025").setMessage(story).setPositiveButton("Back to business",null).create());}
    class Board extends View {
        final Paint p=new Paint(3);Canvas c;final RectF map=new RectF(18,100,762,540);
        final int bg=0xff101d2c,panel=0xff1c2d40,muted=0xff91a5b7,white=0xfff4f1e8,amber=0xffffc566,mint=0xff73d6cf;
        int mode=0;long last=0,lastSave=0;float scale=1,ox=0,oy=0;
        Board(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        float x(int cell){return map.left+(cell%Game.W+.5f)*24;}
        float y(int cell){return map.top+(cell/Game.W+.5f)*(440f/21);}
        void box(float l,float t,float r,float b,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,radius,radius,p);}
        void text(String s,float x,float y,float size,int color,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
        void line(float x,float y,float xx,float yy,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);}
        void dot(float x,float y,float r,int color){p.setColor(color);c.drawCircle(x,y,r,p);}
        String shortText(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
        void wrap(String s,float x,float y,int chars){String row="";for(String w:s.split(" ")){if((row+w).length()>chars){text(row,x,y,12,muted,false);row="";y+=18;}row+=w+" ";}if(!row.isEmpty())text(row,x,y,12,muted,false);}
        @Override protected void onDraw(Canvas canvas){
            long now=System.nanoTime();if(last!=0&&foreground)game.tick((now-last)/1e9);last=now;if(now-lastSave>15_000_000_000L){save();lastSave=now;}
            scale=Math.min(getWidth()/1100f,getHeight()/660f);ox=(getWidth()-1100*scale)/2;oy=(getHeight()-660*scale)/2;c=canvas;c.drawColor(bg);c.save();c.translate(ox,oy);c.scale(scale,scale);box(0,0,1100,660,bg,0);
            text("LANTERN",20,37,29,white,true);text("M O B I L I T Y   E M P I R E",22,62,10,amber,true);
            text("COMPANY CASH",298,24,10,muted,true);text(Game.money(game.cash),298,54,24,game.cash<0?0xffff998b:white,true);
            text("PLATFORM INCOME",525,24,10,muted,true);text(Game.money(game.revenue),525,54,24,white,true);
            text("RATING",768,24,10,muted,true);text(String.format(Locale.US,"%.2f / 5",game.rating),768,54,24,mint,true);
            text("YEAR",983,24,10,muted,true);text(""+game.year(),983,54,24,amber,true);
            text("BENGALURU / LOCAL NETWORK SCHEMATIC",20,88,10,muted,true);text(game.trafficName(),570,88,10,game.rushHour()?amber:mint,true);
            drawMap();side();box(18,550,1082,580,panel,6);text(shortText(game.message,147),29,570,12,white,false);
            String[] labels={"DISPATCH","FLEET","EXPAND","MARKET","HISTORY","SPEED "+(game.speed==0?"II":game.speed+"x"),"HELP","NEW"};
            for(int i=0;i<8;i++){float x=18+i*134;box(x,594,x+124,643,i<4&&mode==i?amber:panel,8);text(labels[i],x+12,623,12,i<4&&mode==i?bg:white,true);}
            c.restore();if(foreground)postInvalidateOnAnimation();
        }
        void drawMap(){
            box(map.left,map.top,map.right,map.bottom,0xff243748,8);
            for(int cell=0;cell<Game.W*Game.H;cell++){
                int xx=cell%Game.W,yy=cell/Game.W;float x=x(cell),y=y(cell);
                if(Game.road(xx,yy)){box(x-12,y-10.5f,x+12,y+10.5f,0xff142333,0);if(xx%5==0&&yy%4!=0)line(x,y-4,x,y+4,0xff3a4a52,1);if(yy%4==0&&xx%5!=0)line(x-4,y,x+4,y,0xff3a4a52,1);}
                else if((xx*13+yy*7)%19==0){dot(x,y,6,0xff3e6459);dot(x+3,y-2,4,0xff527460);}
                else {int h=(xx*19+yy*11)%3;box(x-9,y-7,x+8,y+7,h==0?0xff344b60:h==1?0xff2b4257:0xff3b4f62,2);box(x-6,y-5,x+5,y-3,0xff486079,1);if((xx+yy)%3==0)box(x+1,y,x+4,y+3,0xffd1aa66,0);}
            }
            for(Game.Taxi t:game.taxis)if(t.status==Game.PICKUP||t.status==Game.RIDING){for(int i=t.at+1;i<t.path.length;i++)line(x(t.path[i-1]),y(t.path[i-1]),x(t.path[i]),y(t.path[i]),t.status==Game.RIDING?0x6673d6cf:0x66ffc566,3);}
            for(int i=0;i<game.districts.size();i++){
                Game.District d=game.districts.get(i);float x=x(d.cell()),y=y(d.cell());dot(x,y,15,d.open?d.color:0xff556170);dot(x,y,10,bg);text(d.open?""+(i+1):"+",x-4,y+5,13,d.open?d.color:muted,true);
                p.setTextSize(11);p.setTypeface(Typeface.DEFAULT_BOLD);float w=p.measureText(d.name);box(x-w/2-5,y+19,x+w/2+5,y+38,0xee101d2c,4);text(d.name,x-w/2,y+33,11,white,true);
                int waiting=0;for(Game.Ride r:game.requests)if(r.from==i)waiting++;if(waiting>0){dot(x+13,y-13,8,amber);text(""+waiting,x+10,y-10,9,bg,true);}
            }
            for(Game.Taxi t:game.taxis){
                int a=t.cell,b=t.path[Math.min(t.at+1,t.path.length-1)];float x=x(a)+(x(b)-x(a))*(float)t.progress,y=y(a)+(y(b)-y(a))*(float)t.progress;
                c.save();if(a!=b)c.rotate((float)Math.toDegrees(Math.atan2(y(b)-y(a),x(b)-x(a))),x,y);
                box(x-9,y-5,x+9,y+5,t.status==Game.RIDING?mint:t.status==Game.CHARGING?0xffb4a0e5:amber,3);box(x-3,y-4,x+4,y+4,bg,2);box(x-8,y-7,x-3,y-5,bg,1);box(x+3,y+5,x+8,y+7,bg,1);c.restore();
            }
        }
        void side(){
            box(782,100,1082,540,panel,10);
            if(mode==0){text("LIVE RIDE REQUESTS",798,126,13,white,true);text(game.idleCount()+" available / "+game.taxis.size()+" drivers",798,146,11,muted,false);
                for(int i=0;i<Math.min(4,game.requests.size());i++){Game.Ride r=game.requests.get(i);float yy=158+i*76;box(796,yy,1068,yy+66,0xff293e52,7);text(game.districts.get(r.from).name,808,yy+21,13,white,true);text("to "+game.districts.get(r.to).name,808,yy+40,11,muted,false);text(Game.money(r.fare)+" gross fare",808,yy+56,10,amber,true);float remaining=(float)Math.max(0,1-(game.elapsed-r.created)/65);box(1058,yy+8,1062,yy+8+49*remaining,mint,2);}
                if(game.requests.isEmpty())wrap("The queue is clear. New requests arrive as the simulation runs.",799,186,34);
                box(796,474,1068,520,game.autoDispatch?0xff305951:0xff293e52,7);text("AUTO DISPATCH: "+(game.autoDispatch?"ON":"OFF"),808,502,12,game.autoDispatch?mint:amber,true);
            }
            if(mode==1){text("DRIVER NETWORK",798,128,13,white,true);text(game.taxis.size()+" / 12 partners",798,157,22,amber,true);text("Completed rides: "+game.rides,798,184,13,white,false);text("Missed requests: "+game.missed,798,206,12,muted,false);text("Running cost: "+Game.money(game.taxis.size()*2)+" / second",798,228,12,muted,false);
                box(796,252,1068,300,0xff324959,7);text("RECRUIT DRIVER",808,273,12,white,true);text(Game.money(game.taxiPrice())+" onboarding cost",808,291,11,amber,false);
                text("BUSINESS MODEL",798,330,10,muted,true);text(game.modelName(),798,356,19,mint,true);wrap("Fare income is shared with drivers. Subscriptions trade ride commissions for fixed fees.",798,382,33);
                box(796,470,1068,520,0xff324959,7);text("CHANGE BUSINESS MODEL",808,500,12,white,true);
            }
            if(mode==2){text("BENGALURU SERVICE AREA",798,128,13,white,true);for(int i=0;i<6;i++){Game.District d=game.districts.get(i);float yy=144+i*44;box(796,yy,1068,yy+38,0xff293e52,5);text(d.name,806,yy+16,12,d.open?white:muted,true);text(d.open?"Operating":Game.money(game.districtPrice(i))+" launch cost",806,yy+31,10,d.open?mint:amber,false);}
                text("NATIONAL NETWORK: "+game.branchCount()+" / 6 CITIES",798,438,11,mint,true);box(796,470,1068,520,0xff324959,7);text("OPEN NATIONAL EXPANSION",808,500,12,white,true);
            }
            if(mode==3){text("FICTIONAL SHARE EXCHANGE",798,128,12,white,true);for(int i=0;i<3;i++){Game.Company v=game.companies.get(i);float yy=145+i*76;box(796,yy,1068,yy+66,0xff293e52,7);text(v.name,808,yy+21,13,white,true);text(v.merged?"Acquired · integrated":Game.money(v.price)+" / share · "+v.owned+"% owned",808,yy+43,11,v.merged?mint:amber,false);if(v.priceHistory.size()>1){int lo=Collections.min(v.priceHistory),hi=Collections.max(v.priceHistory);for(int h=1;h<v.priceHistory.size();h++){float x1=1003+(h-1)*53f/(v.priceHistory.size()-1),x2=1003+h*53f/(v.priceHistory.size()-1);float y1=yy+27-(v.priceHistory.get(h-1)-lo)*18f/Math.max(1,hi-lo),y2=yy+27-(v.priceHistory.get(h)-lo)*18f/Math.max(1,hi-lo);line(x1,y1,x2,y2,mint,1.5f);}}}
                text("PORTFOLIO: "+Game.money(game.portfolioValue()),798,399,12,mint,true);text("MARGIN DEBT: "+Game.money(game.marginDebt),798,422,12,game.marginDebt>0?amber:muted,true);text("EQUITY: "+Math.round(game.equityRatio()*100)+"% · CALLS: "+game.marginCalls,798,445,11,muted,false);wrap("Tap a company to trade or acquire it. Prices and rules are invented for this game.",798,478,34);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float px=(e.getX()-ox)/scale,py=(e.getY()-oy)/scale;
            if(py>=594&&py<=643){for(int i=0;i<8;i++)if(px>=18+i*134&&px<=142+i*134){if(i<4)mode=i;if(i==4)history();if(i==5){game.speed=game.speed==1?2:game.speed==2?4:game.speed==4?0:1;game.message=game.speed==0?"Paused. You can still manage the company.":"Simulation speed: "+game.speed+"x.";}if(i==6)help();if(i==7)show(new AlertDialog.Builder(MainActivity.this).setTitle("Start a new company?").setMessage("This replaces your current saved company.").setNegativeButton("Keep playing",null).setPositiveButton("Start again",(d,w)->{game=new Game();mode=0;save();}).create());performClick();return true;}}
            if(px>=796&&px<=1068){
                if(mode==0){if(py>=474&&py<=520){game.autoDispatch=!game.autoDispatch;game.message=game.autoDispatch?"Auto dispatch assigns the nearest idle driver.":"Manual dispatch enabled.";}else for(int i=0;i<Math.min(4,game.requests.size());i++)if(py>=158+i*76&&py<=224+i*76){game.dispatch(game.requests.get(i).id);break;}}
                if(mode==1){if(py>=252&&py<=300)show(new AlertDialog.Builder(MainActivity.this).setTitle("Recruit a driver partner?").setMessage("Onboarding: "+Game.money(game.taxiPrice())+"\nPlatform running cost: 2 cr per game second.\nFleet limit: 12 partners.").setNegativeButton("Cancel",null).setPositiveButton("Recruit",(d,w)->game.buyTaxi()).create());if(py>=470&&py<=520)models();}
                if(mode==2){for(int i=0;i<6;i++)if(py>=144+i*44&&py<=182+i*44){district(i);break;}if(py>=470&&py<=520)regions();}
                if(mode==3)for(int i=0;i<3;i++)if(py>=145+i*76&&py<=211+i*76)company(i);
                save();performClick();return true;
            }
            if(map.contains(px,py)){int nearest=-1;double distance=32;for(int i=0;i<game.districts.size();i++){Game.District d=game.districts.get(i);double dd=Math.hypot(px-x(d.cell()),py-y(d.cell()));if(dd<distance){distance=dd;nearest=i;}}if(nearest>=0){if(mode==2||!game.districts.get(nearest).open)district(nearest);else {Game.Ride chosen=null;for(Game.Ride r:game.requests)if(r.from==nearest){chosen=r;break;}if(chosen!=null)game.dispatch(chosen.id);else game.message="No passengers waiting at "+game.districts.get(nearest).name+".";}}}
            performClick();return true;
        }
        void district(int i){Game.District d=game.districts.get(i);if(d.open){game.message=d.name+" is open: "+d.landmark+".";return;}show(new AlertDialog.Builder(MainActivity.this).setTitle("Launch "+d.name+"?").setMessage("Open a service desk and recruit local demand.\nCost: "+Game.money(game.districtPrice(i))).setNegativeButton("Cancel",null).setPositiveButton("Launch district",(dialog,w)->game.unlock(i)).create());}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
