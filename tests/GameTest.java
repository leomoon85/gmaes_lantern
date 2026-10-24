package com.lantern.dispatch;

public class GameTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void run(Game g,int steps){for(int i=0;i<steps;i++)g.tick(.2);}
    public static void main(String[] args){
        Game g=new Game();
        for(Game.District a:g.districts)for(Game.District b:g.districts){int[] path=g.route(a.cell(),b.cell());check(path!=null,"Every district reachable");for(int i=1;i<path.length;i++){int x=path[i]%Game.W,y=path[i]/Game.W;check(Game.road(x,y),"Path must follow roads");check(Math.abs(x-path[i-1]%Game.W)+Math.abs(y-path[i-1]/Game.W)==1,"Adjacent road cells");}}
        check(g.dispatch(g.requests.get(0).id),"Available driver dispatches");check(!g.dispatch(g.requests.get(0).id),"Busy driver cannot double book");run(g,100);check(g.rides>0&&g.revenue>0&&g.grossFares>g.revenue,"Fare split and completed ride");
        double time=g.elapsed;g.speed=0;g.tick(.2);check(g.elapsed==time,"Paused clock");
        Game poor=new Game();poor.cash=0;check(!poor.buyTaxi()&&poor.taxis.size()==1,"Unaffordable driver purchase atomic");check(!poor.unlock(2)&&!poor.districts.get(2).open,"Unaffordable district atomic");
        Game market=new Game();long cash=market.cash;check(market.buyShares(0,true),"Margin buy");check(market.companies.get(0).owned==10&&market.marginDebt==275&&cash-market.cash==275,"50% loan accounting");check(market.sellShares(0),"Sell shares");check(market.marginDebt==0&&market.cash==cash,"Sale repays loan first");
        market.buyShares(0,true);market.companies.get(0).price=15;market.maintainMargin();check(market.marginCalls==1&&market.marginDebt==0&&market.portfolioValue()==0,"Margin liquidation");check(market.cash==cash-400,"Negative equity shortfall charged to cash");
        Game merger=new Game();merger.cash=100000;for(int i=0;i<6;i++)check(merger.buyShares(1,false),"Acquire shares");int fleet=merger.taxis.size();long before=merger.cash,price=merger.mergerPrice(1),treasury=merger.companies.get(1).cash;check(merger.merge(1),"Controlling stake merger");check(merger.taxis.size()==fleet+4&&merger.districts.get(2).open,"Merge fleet and territory");check(merger.cash==before-price+treasury,"Merger consideration and treasury");check(!merger.merge(1),"Cannot acquire twice");
        Game debt=new Game();for(int i=0;i<6;i++)debt.buyShares(0,true);check(!debt.merge(0),"Debt blocks acquisition");debt.repayMargin();check(debt.marginDebt==0,"Repay debt");
        Game history=new Game();check(!history.setModel(2),"Historical model gate");history.elapsed=9*60-.1;history.tick(.2);check(history.year()==2020&&history.historyStage==3,"Historical event progression");int crash=history.companies.get(0).price;history.tick(.2);check(history.companies.get(0).price==crash,"Shock is not repeated");check(history.demandFactor()==.22,"Lockdown demand scenario");history.elapsed=11*60;history.tick(.2);check(history.setModel(2),"Subscription unlock");
        Game expansion=new Game();expansion.cash=100000;check(!expansion.expand(1),"Expansion needs track record");expansion.unlock(2);expansion.rides=20;check(expansion.expand(1)&&expansion.branchCount()==2,"Launch regional branch");check(!expansion.expand(1),"No duplicate launch");
        Game auto=new Game();auto.autoDispatch=true;run(auto,400);check(auto.rides>0,"Auto dispatch serves passengers");
        Game goal=new Game();goal.cash=1000000;goal.rides=100;for(int i=2;i<6;i++)goal.unlock(i);for(int i=1;i<6;i++)goal.expand(i);for(int i=0;i<9;i++)goal.buyTaxi();goal.tick(.2);check(goal.milestone,"National objective achievable");long awarded=goal.cash;goal.tick(.2);check(goal.cash==awarded,"One-time national grant");
        System.out.println("PASS: roads, dispatch, fares, pause, funds, margin accounting/liquidation, mergers, history, subscriptions, expansion, auto dispatch, national goal");
    }
}
