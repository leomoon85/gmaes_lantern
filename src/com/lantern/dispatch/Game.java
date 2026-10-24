package com.lantern.dispatch;

import java.util.*;

/** Original offline taxi-company simulation; no real transport service or external assets. */
public final class Game {
    public static final int W=31,H=21,MAX_FLEET=12;
    public static final int IDLE=0,PICKUP=1,RIDING=2,CHARGING=3;
    public final ArrayList<District> districts=new ArrayList<>();
    public final ArrayList<Taxi> taxis=new ArrayList<>();
    public final ArrayList<Ride> requests=new ArrayList<>();
    public long cash=12000,revenue=0,expenses=0,rides=0,missed=0;
    public double elapsed=0,spawnClock=0,costClock=0,rating=4.8,marketClock=0,competitionClock=0;
    public long marginDebt=0,dividends=0;public int historyStage=0,marginCalls=0,marketTick=0;
    public final ArrayList<Company> companies=new ArrayList<>();
    public int speed=1,nextRide=1,spawnSequence=0;
    public boolean autoDispatch=false,milestone=false;
    public int businessModel=0;public long grossFares=0;
    public final String[] regions={"Bengaluru","Chennai","Hyderabad","Pune","Mumbai","Delhi"};
    public final boolean[] branches={true,false,false,false,false,false};
    public String message="Tap a ride card to send your nearest available cab.";
    public static final class District {
        public final String name,landmark;public final int x,y,color;
        public boolean open;
        District(String n,String l,int x,int y,int color,boolean open){name=n;landmark=l;this.x=x;this.y=y;this.color=color;this.open=open;}
        public int cell(){return y*W+x;}
    }
    public static final class Ride {
        public int id,from,to;public long fare;public double created;
        public Ride(int id,int from,int to,long fare,double created){this.id=id;this.from=from;this.to=to;this.fare=fare;this.created=created;}
    }
    public static final class Taxi {
        public int id,cell,status=IDLE,at=0;public double progress=0,charge=0;
        public int[] path;public Ride ride;
        public Taxi(int id,int cell){this.id=id;this.cell=cell;path=new int[]{cell};}
    }
    public static final class Company {
        public final String name;public final int district;
        public int price,owned=0,fleet;public long cash=6000,served=0;public boolean merged=false;
        public final ArrayList<Integer> priceHistory=new ArrayList<>();
        public Company(String name,int price,int fleet,int district){this.name=name;this.price=price;this.fleet=fleet;this.district=district;priceHistory.add(price);}
        public void recordPrice(){priceHistory.add(price);if(priceHistory.size()>18)priceHistory.remove(0);}
    }
    public Game(){
        districts.add(new District("Central Market","Bengaluru hub",5,4,0xffffc566,true));
        districts.add(new District("Tech Gardens","Office corridor",15,4,0xff73d6cf,true));
        districts.add(new District("Eastern Gateway","Airport corridor",25,4,0xffb4a0e5,false));
        districts.add(new District("Western Exchange","Railway interchange",5,16,0xffe89b7c,false));
        districts.add(new District("Plateau Junction","Residential hub",15,16,0xff93c5a1,false));
        districts.add(new District("Capital Circle","University quarter",25,16,0xff83bce4,false));
        taxis.add(new Taxi(1,districts.get(0).cell()));
        companies.add(new Company("Copperwheel Mobility",55,3,3));
        companies.add(new Company("Glasswing Transit",70,4,2));
        companies.add(new Company("Violet Loop",45,2,5));
        createRide(0,1);createRide(1,0);
    }
    public static boolean road(int x,int y){return x%5==0||y%4==0;}
    public int openCount(){int n=0;for(District d:districts)if(d.open)n++;return n;}
    public int idleCount(){int n=0;for(Taxi t:taxis)if(t.status==IDLE)n++;return n;}
    public int branchCount(){int n=0;for(boolean b:branches)if(b)n++;return n;}
    public long branchPrice(int i){return 5000+i*1500L;}
    public boolean expand(int i){
        if(i<0||i>=branches.length||branches[i])return false;
        if(openCount()<3||rides<20){message="Prove the model first: serve 3 districts and complete 20 rides.";return false;}
        if(cash<branchPrice(i)){message="Need "+money(branchPrice(i))+" for this city launch.";return false;}
        spend(branchPrice(i));branches[i]=true;message="Launched "+regions[i]+". Regional operations now contribute each quarter.";return true;
    }
    public String modelName(){return new String[]{"Commission","Corporate commute","Driver subscription"}[businessModel];}
    public boolean setModel(int value){
        if(value<0||value>2)return false;
        if(value==1&&year()<2015||value==2&&year()<2022){message="This model is not available yet. See HISTORY for the timeline.";return false;}
        businessModel=value;message="Business model changed to "+modelName()+".";return true;
    }
    public int trafficPhase(){return ((int)(elapsed/45))%3;}
    public boolean rushHour(){return trafficPhase()==1;}
    public String trafficName(){return rushHour()?"RUSH HOUR":"CLEAR STREETS";}
    public double trafficFactor(){return rushHour()?.65:1;}
    public int year(){return Math.min(2025,2011+(int)(elapsed/60));}
    public double demandFactor(){double historical=year()==2020?.22:year()==2021?.65:year()>=2015?1.15:1;return historical*(businessModel==1?(rushHour()?1.6:.72):businessModel==2?1.25:1);}
    public boolean bankHoliday(){return false;}
    public long taxiPrice(){return Math.round((2500L+(taxis.size()-1)*450L)*(year()>=2015?.85:1));}
    public long districtPrice(int i){return 3500L+i*600L;}
    public int[] route(int start,int goal){
        if(start<0||goal<0||start>=W*H||goal>=W*H)return null;
        int[] prev=new int[W*H];Arrays.fill(prev,-2);prev[start]=-1;
        ArrayDeque<Integer> q=new ArrayDeque<>();q.add(start);
        while(!q.isEmpty()){
            int a=q.remove();if(a==goal)break;int x=a%W,y=a/W;
            for(int b:new int[]{x>0?a-1:-1,x<W-1?a+1:-1,y>0?a-W:-1,y<H-1?a+W:-1}){
                if(b<0||!road(b%W,b/W)||prev[b]!=-2)continue;prev[b]=a;q.add(b);
            }
        }
        if(prev[goal]==-2)return null;
        ArrayList<Integer> cells=new ArrayList<>();for(int a=goal;a!=-1;a=prev[a])cells.add(a);
        Collections.reverse(cells);int[] out=new int[cells.size()];for(int i=0;i<out.length;i++)out[i]=cells.get(i);return out;
    }
    public void createRide(int from,int to){
        if(from==to||!districts.get(from).open||!districts.get(to).open||requests.size()>=6)return;
        int distance=route(districts.get(from).cell(),districts.get(to).cell()).length-1;
        long fare=Math.round((90+distance*15+to*12)*(rushHour()?1.25:1));
        requests.add(new Ride(nextRide++,from,to,fare,elapsed));
    }
    private void generateRide(){
        ArrayList<Integer> open=new ArrayList<>();for(int i=0;i<districts.size();i++)if(districts.get(i).open)open.add(i);
        int seq=spawnSequence++;int a=open.get((seq*7+seq/3)%open.size());
        int bi=(open.indexOf(a)+1+(seq/open.size())%(open.size()-1))%open.size();createRide(a,open.get(bi));
    }
    private void spend(long n){cash-=n;expenses+=n;}
    public boolean buyTaxi(){
        if(taxis.size()>=MAX_FLEET){message="Your 12-cab fleet is complete.";return false;}
        long price=taxiPrice();if(cash<price){message="Need "+money(price)+" to recruit the next driver partner.";return false;}
        spend(price);taxis.add(new Taxi(taxis.size()+1,districts.get(0).cell()));message="New driver partner ready at Central Market.";return true;
    }
    public boolean unlock(int i){
        if(i<0||i>=districts.size())return false;District d=districts.get(i);
        if(d.open){message=d.name+" is already in your service area.";return false;}
        long cost=districtPrice(i);if(cash<cost){message="Need "+money(cost)+" to open "+d.name+".";return false;}
        spend(cost);d.open=true;createRide(i,0);message="Welcome to "+d.name+". New passengers are waiting.";return true;
    }
    public boolean dispatch(int rideId){
        Ride r=null;for(Ride v:requests)if(v.id==rideId){r=v;break;}
        if(r==null)return false;
        Taxi nearest=null;int[] best=null;
        for(Taxi t:taxis)if(t.status==IDLE){int[] path=route(t.cell,districts.get(r.from).cell());if(path!=null&&(best==null||path.length<best.length)){nearest=t;best=path;}}
        if(nearest==null){message="All cabs are busy. Buy another or wait for a drop-off.";return false;}
        nearest.ride=r;nearest.path=best;nearest.at=0;nearest.progress=0;nearest.status=PICKUP;requests.remove(r);
        message="Cab "+nearest.id+" heading to "+districts.get(r.from).name+".";
        if(best.length==1)arrive(nearest);
        return true;
    }
    private void arrive(Taxi t){
        if(t.status==PICKUP){
            if(elapsed-t.ride.created>55)rating=Math.max(1,rating-.08);
            t.status=RIDING;t.path=route(t.cell,districts.get(t.ride.to).cell());t.at=0;t.progress=0;
        }else if(t.status==RIDING){
            long income=businessModel==2?0:Math.round(t.ride.fare*(businessModel==1?.40:.32));
            grossFares+=t.ride.fare;cash+=income;revenue+=income;rides++;rating=Math.min(5,rating+(businessModel==2?.025:.015));
            message="Cab "+t.id+" completed a ride. Platform income: "+money(income)+".";
            t.ride=null;t.status=CHARGING;t.charge=3;t.path=new int[]{t.cell};t.at=0;t.progress=0;
        }
    }
    public long portfolioValue(){long n=0;for(Company c:companies)if(!c.merged)n+=(long)c.price*c.owned;return n;}
    public long netWorth(){return cash+portfolioValue()-marginDebt+taxis.size()*1500L+openCount()*2000L+(branchCount()-1)*4000L;}
    public double equityRatio(){long v=portfolioValue();return v==0?1:(double)(v-marginDebt)/v;}
    public boolean buyShares(int index,boolean margin){
        Company c=companies.get(index);if(c.merged||c.owned>=100){message="No shares available.";return false;}
        if(bankHoliday()){message="Bank holiday: share dealing is temporarily suspended.";return false;}
        long full=c.price*10L,borrow=margin?full/2:0,cost=full-borrow;
        if(cash<cost){message="Insufficient company cash for this purchase.";return false;}
        if(margin&&(double)(portfolioValue()+full-marginDebt-borrow)/(portfolioValue()+full)<.5){message="Reduce margin debt before increasing leverage.";return false;}
        cash-=cost;marginDebt+=borrow;c.owned+=10;message="Bought 10 shares of "+c.name+(margin?" using 50% borrowed funds.":" for cash.");return true;
    }
    public boolean sellShares(int index){
        Company c=companies.get(index);if(c.merged||c.owned<10){message="No shares available to sell.";return false;}
        if(bankHoliday()){message="Bank holiday: share dealing is temporarily suspended.";return false;}
        long proceeds=c.price*10L,repay=Math.min(marginDebt,proceeds);marginDebt-=repay;cash+=proceeds-repay;c.owned-=10;
        message="Sold 10 shares. Proceeds repay margin debt first.";return true;
    }
    public void repayMargin(){long n=Math.min(Math.max(0,cash),marginDebt);cash-=n;marginDebt-=n;message=n>0?"Repaid "+money(n)+" of margin debt.":"No cash or margin debt to repay.";}
    public long mergerPrice(int i){Company c=companies.get(i);return (long)Math.ceil((100-c.owned)*c.price*1.15)+1200;}
    public boolean merge(int i){
        Company c=companies.get(i);if(c.merged||c.owned<60){message="Acquire at least 60% of the voting shares first.";return false;}
        if(bankHoliday()){message="The bank holiday suspends merger financing.";return false;}
        if(marginDebt>0){message="Repay all margin debt before completing a merger.";return false;}
        long cost=mergerPrice(i);if(cash<cost){message="Need "+money(cost)+" for the remaining shares and integration.";return false;}
        cash-=cost;int inherited=Math.min(c.fleet,MAX_FLEET-taxis.size());
        for(int n=0;n<inherited;n++)taxis.add(new Taxi(taxis.size()+1,districts.get(c.district).cell()));
        cash+=c.cash+(c.fleet-inherited)*1500L;districts.get(c.district).open=true;c.owned=0;c.merged=true;
        message="Merged "+c.name+": fleet, treasury and district integrated.";return true;
    }
    public void maintainMargin(){
        long v=portfolioValue();if(marginDebt==0)return;
        if(v==0||equityRatio()<.30){
            cash+=v-marginDebt;marginDebt=0;for(Company c:companies)c.owned=0;marginCalls++;
            message="Margin call: broker liquidated the portfolio. Any shortfall is charged to company cash.";
        }
    }
    private void history(){
        if(historyStage==0&&year()>=2013){historyStage=1;message="2013: international app-based competition enters Bengaluru. Build service quality before expanding.";}
        if(historyStage==1&&year()>=2015){historyStage=2;message="2015: consolidation, bike taxis and carpooling reshape mobility. Driver onboarding is now 15% cheaper in this scenario.";}
        if(historyStage==2&&year()>=2020){historyStage=3;for(Company c:companies)if(!c.merged){c.price=Math.max(5,(int)Math.round(c.price*.48));c.recordPrice();}message="2020: lockdown disrupts travel. Scenario demand drops; fictional share prices fall 52%. Check your margin.";maintainMargin();}
        if(historyStage==3&&year()>=2022){historyStage=4;message="2022: driver-direct auto platforms emerge in Bengaluru. A subscription strategy is now available.";}
    }
    private void market(){
        marketTick++;
        long regional=Math.round((branchCount()-1)*220*demandFactor());cash+=regional;revenue+=regional;
        if(businessModel==2){long subscription=taxis.size()*80L;cash+=subscription;revenue+=subscription;}
        for(int i=0;i<companies.size();i++){
            Company c=companies.get(i);if(c.merged)continue;
            double wave=Math.sin((marketTick+i*3)*1.7)*.035;
            double trend=year()==2020?-.012:.014;
            double fundamental=(c.cash+c.fleet*1800.0)/100;
            double valuePull=Math.max(-.025,Math.min(.025,(fundamental/c.price-1)*.04));
            c.price=Math.max(5,(int)Math.round(c.price*(1+trend+wave+valuePull)));c.recordPrice();
            long dividend=c.owned*(c.cash>2500?1:0);cash+=dividend;dividends+=dividend;c.cash-=dividend;
        }
        long interest=(marginDebt+49)/50;cash-=interest;expenses+=interest;maintainMargin();
    }
    public void tick(double seconds){
        if(speed==0||seconds<=0)return;double dt=Math.min(.25,seconds)*speed;elapsed+=dt;spawnClock+=dt;costClock+=dt;marketClock+=dt;competitionClock+=dt;
        history();
        double interval=Math.max(3,9-openCount())/demandFactor();
        while(spawnClock>=interval){spawnClock-=interval;generateRide();}
        while(marketClock>=15){marketClock-=15;market();}
        while(competitionClock>=16){competitionClock-=16;for(Company c:companies)if(!c.merged){
            c.cash+=c.fleet*18;c.served++;
            if(!requests.isEmpty()){requests.remove(0);message=c.name+" won a waiting fare. Dispatch sooner or acquire the rival.";}
        }}
        while(costClock>=1){costClock--;spend(taxis.size()*2L);}
        for(Iterator<Ride> it=requests.iterator();it.hasNext();){Ride r=it.next();if(elapsed-r.created>65){it.remove();missed++;rating=Math.max(1,rating-.04);}}
        for(Taxi t:taxis){
            if(t.status==CHARGING){t.charge-=dt;if(t.charge<=0){t.status=IDLE;t.charge=0;}continue;}
            if(t.status!=PICKUP&&t.status!=RIDING)continue;
            t.progress+=dt*2.4*trafficFactor();
            while(t.progress>=1&&(t.status==PICKUP||t.status==RIDING)){
                t.progress--;t.at++;t.cell=t.path[t.at];
                if(t.at==t.path.length-1){arrive(t);break;}
            }
        }
        if(autoDispatch){ArrayList<Ride> waiting=new ArrayList<>(requests);for(Ride r:waiting){if(idleCount()==0)break;dispatch(r.id);}}
        if(!milestone&&branchCount()==6&&taxis.size()>=10&&rides>=100){milestone=true;cash+=15000;message="National network achieved! 15,000 cr growth grant received.";}
    }
    public static String money(long n){return String.format(Locale.US,"%,d cr",n);}
}
