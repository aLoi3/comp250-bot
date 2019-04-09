package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class MyBot extends AbstractionLayerAI {    
    private UnitTypeTable utt;
    private UnitType worker;
    private UnitType archer;
    private UnitType light;
    private UnitType heavy;
    private UnitType barracks;
    private UnitType base;
    
    public MyBot(UnitTypeTable utt) {
        super(new AStarPathFinding());
        this.utt = utt;
        worker = utt.getUnitType("Worker");
        archer = utt.getUnitType("Archer");
        light = utt.getUnitType("Light");
        heavy = utt.getUnitType("Heavy");
        barracks = utt.getUnitType("Barracks");
        base = utt.getUnitType("Base");
    }
    

    @Override
    public void reset() {
    }

    
    @Override
    public AI clone() {
        return new MyBot(utt);
    }
   
    
    @Override
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        
        // Harvest worker behaviour
        for (Unit unit : pgs.getUnits()) {
            // TODO: issue commands to units
        	if(unit.getPlayer() == player && unit.getType().canHarvest)
        	{
        		
        	}
        	
        }
        
        return translateActions(player, gs);
    }
    
    public Unit GetClosestEnemy(PhysicalGameState pgs, Player p, Unit u)
    {
    	Unit closestEnemy = null;
    	int closestDistance = 0;
    	for(Unit u2:pgs.getUnits())
    	{
    		if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
    		{
    			int distance = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
    			if(closestEnemy == null || distance < closestDistance)
    			{
    				closestEnemy = u2;
    				closestDistance = distance;
    			}
    		}
    	}
    	return closestEnemy;
    }
    
    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }
    
    public void WorkerHarvest(List<Unit> workers, Player p, PhysicalGameState pgs)
    {
    	int nBases = 0;
    	int nBarracks = 0;
    	
    	List<Unit> freeWorkers = new LinkedList<Unit>();
    	freeWorkers.addAll(workers);
    	
    	if (workers.isEmpty())
    	{
    		return;
    	}
    	
    	for (Unit u1 : pgs.getUnits())
    	{    		
    		if(u1.getType() == base && u1.getPlayer() == p.getID())
    		{
    			nBases++;
    		}
    		
    		if(u1.getType() == barracks && u1.getPlayer() == p.getID())
    		{
    			nBarracks++;
    		}
    	}
    	
    	List<Integer> reservedPositions = new LinkedList<Integer>();
    	if (nBases == 0 && !freeWorkers.isEmpty())
    	{
    		if(p.getResources() >= base.cost)
    		{
    			Unit u = freeWorkers.remove(0);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    			
    		}	
    	}
    	if (nBarracks == 0 && !freeWorkers.isEmpty())
    	{
    		if(p.getResources() >= barracks.cost)
    		{
    			Unit u = freeWorkers.remove(0);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    			
    		}
    	}
    	
    	// harvest with all the free workers:
        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } else {
                    harvest(u, closestResource, closestBase);
                }
            }
        }
    }
    
}
