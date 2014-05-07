package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleMonteCarloGamer is a simple state-machine-based Gamer. It will use a
 * pure Monte Carlo approach towards picking moves, doing simulations and then
 * choosing the move that has the highest expected score. It should be slightly
 * more challenging than the RandomGamer, while still playing reasonably fast.
 *
 * However, right now it isn't challenging at all. It's extremely mediocre, and
 * doesn't even block obvious one-move wins. This is partially due to the speed
 * of the default state machine (which is slow) and mostly due to the algorithm
 * assuming that the opponent plays completely randomly, which is inaccurate.
 *
 * @author Adriana
 */
public final class MCTSPimp extends SampleGamer
{

	private static final int SELECT_CONST = 10;
	private static final int NUM_CHARGES = 1;
	private static final int MC_TIMEOUT_MARGIN = 1000;
	private static final int SHORT_TIMEOUT_MARGIN = 30;
	private Map<MachineState, Integer> numVisits;
	private Map<MachineState, Integer> totals;
	/**
	 * Employs a simple sample "Monte Carlo" algorithm.
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = doTheMonteCarlo(getRole(), getCurrentState(), timeout);

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}


	private int[] depth = new int[1];
	private Move doTheMonteCarlo(Role role, MachineState currentState,
			long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		StateMachine SM = getStateMachine();
		numVisits = new HashMap<MachineState, Integer>();
		totals = new HashMap<MachineState, Integer>();
		numVisits.put(currentState, 0);
		totals.put(currentState, 0);

		while(true) {
			if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
				break;
			}
			ArrayList<MachineState> path = new ArrayList<MachineState>();//for backprop
			MachineState selectedState = select(currentState, role, path, timeout);

			if (SM.isTerminal(selectedState))  {
				numVisits.put(selectedState, numVisits.get(selectedState) + 1);
				totals.put(selectedState, totals.get(selectedState) +SM.getGoal(selectedState, role));
				continue;
			}
			expand(selectedState, role, timeout);
			//TODO: maybe do it more than once
			int dcScore = 0;
			for (int i=0; i< NUM_CHARGES; i++) {
				dcScore += SM.getGoal(SM.performDepthCharge(selectedState, depth), role);
			}
			if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
				break;
			}
			backpropagate(selectedState, dcScore, path, role, timeout);



		}
		List<Move> legalMoves = SM.getLegalMoves(currentState, role);
		double bestUtility = 0;
		//TODO: check if nonempty
		Move bestMove  = legalMoves.get(0);
		for (int i=0; i < legalMoves.size(); i++) {
			List<List<Move>> legalJointMoves = SM.getLegalJointMoves(currentState, role, legalMoves.get(i));
			for(int j=0; j< legalJointMoves.size(); j++) {
				MachineState child = SM.getNextState(currentState, legalJointMoves.get(j));
				if (numVisits.get(child) == 0) {
					continue;
				}
				double childUtility = totals.get(child)/(double)numVisits.get(child);
				if(childUtility > bestUtility) {
					bestUtility = childUtility;
					bestMove = legalMoves.get(i);
				}
				if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
					return bestMove;
				}
			}
		}



		return bestMove;
	}



	private void backpropagate(MachineState selectedState, int dcScore,
			ArrayList<MachineState> path, Role role, long timeout) throws GoalDefinitionException {
		StateMachine SM = getStateMachine();
		for(int i=0; i< path.size(); i++) {
			MachineState cur = path.get(i);
			numVisits.put(cur, numVisits.get(cur) + NUM_CHARGES);
			totals.put(cur, totals.get(cur) +dcScore);
			if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
				return ;
			}

		}

	}


    /*
     * Expand simply adds the children of the selected node to numVisits and totals
     */
	private void expand(MachineState state, Role role, long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine SM = getStateMachine();
		List<Move> legalMoves = SM.getLegalMoves(state, role);
		for (int i=0; i < legalMoves.size(); i++) {
			List<List<Move>> legalJointMoves = SM.getLegalJointMoves(state, role, legalMoves.get(i));
			for(int j=0; j< legalJointMoves.size(); j++) {
				MachineState child = SM.getNextState(state, legalJointMoves.get(j));
				if(numVisits.get(child) == null) {
					numVisits.put(child, 0);
					totals.put(child, 0);
				}
				if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
					return;
				}
			}
		}
	}



	private MachineState select(MachineState state, Role role, ArrayList<MachineState> path, long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine SM = getStateMachine();
		while (true) {
			path.add(state);
			if (timeout - System.currentTimeMillis() <= MC_TIMEOUT_MARGIN){
				return state;
			}
			if (numVisits.get(state) == 0)  {
				return state;
			}
			//find unvisited child and return it
			List<Move> legalMoves = SM.getLegalMoves(state, role);
			for (int i=0; i < legalMoves.size(); i++) {
				List<List<Move>> legalJointMoves = SM.getLegalJointMoves(state, role, legalMoves.get(i));
				for(int j=0; j< legalJointMoves.size(); j++) {
					MachineState child = SM.getNextState(state, legalJointMoves.get(j));
					if(numVisits.get(child) != null && numVisits.get(child)== 0) {
						path.add(child);
						return child;
					}
				}
			}
			int score = 0;
			MachineState result = state;
			// check which visited child is worth exploring
			for (int i=0; i < legalMoves.size(); i++) {
				List<List<Move>> legalJointMoves = SM.getLegalJointMoves(state, role, legalMoves.get(i));
				for(int j=0; j< legalJointMoves.size(); j++) {
					MachineState child = getStateMachine().getNextState(state, legalJointMoves.get(j));
					if(numVisits.get(child) == null) {
						continue;
					}
					int curScore = selectFn(state, child);
					if (curScore> score) {
						score = curScore;
						result = child;
					}
				}
			}
			state = result;
		}
	}



	private int selectFn(MachineState parent, MachineState child) {
		double utility = totals.get(child)/ (double)numVisits.get(child);// avg reward
		if (numVisits.get(child) == 0) {
			System.out.println("its zero bitch");
		}
		return (int) (utility + SELECT_CONST *Math.sqrt(2*Math.log(numVisits.get(parent))/numVisits.get(child)));
	}

}