package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleLegalGamer is a minimal gamer which always plays the first
 * legal move it identifies, regardless of the state of the game.
 *
 * For your first players, you should extend the class SampleGamer
 * The only function that you are required to override is :
 * public Move stateMachineSelectMove(long timeout)
 *
 */
public final class MonteCarloPimp extends SampleGamer
{
	//Milliseconds before timeout where we must bail
		public final static int TIMEOUT_MARGIN = 50;
		public final static int TIMEOUT_MARGIN2 = 100;
		public final static int LEVEL_LIMIT = 3;
		public final static int NUM_DEPTH_CHARGES = 1000;


	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.
	 *
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		Move selection = bestMove(getRole(), getCurrentState(), timeout);

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private Move bestMove(Role role, MachineState state, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		Move bestAction = legalMoves.get(0);
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);
			int result = minScore(role, move, state, timeout, 0);
			if (result > score) {
				score = result;
				bestAction = move;
			}
			if (timeout - System.currentTimeMillis() <= TIMEOUT_MARGIN){
				break;
			}
		}
		return bestAction;
	}



	private int  maxScore(Role role, MachineState state, long timeout, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state))  {
			return getStateMachine().getGoal(state, role);
		}
		if (level> LEVEL_LIMIT) {
			return monteCarlo(role, state, NUM_DEPTH_CHARGES, timeout);
		}

		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			Move curMove = legalMoves.get(i);

			int result = minScore(role, curMove, state, timeout, level);
			if (result == 100) {
				return 100;
			}
			if (result > score) {
				score = result;
			}
			if (timeout - System.currentTimeMillis() <= TIMEOUT_MARGIN){
				break;
			}
		}
		return score;

	}
	private int  minScore(Role role, Move move, MachineState state, long timeout, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

		List<List<Move>> legalJointMoves = getStateMachine().getLegalJointMoves(state, role, move);
		int score = 100;
		for (int i = 0; i < legalJointMoves.size(); i++) {
			List<Move> currMoves = legalJointMoves.get(i);
			MachineState nextState = getStateMachine().getNextState(state, currMoves);
			int result = maxScore(role, nextState, timeout, level + 1);
			if (result < score) {
				score = result;
			}
			if (timeout - System.currentTimeMillis() <= TIMEOUT_MARGIN){
				break;
			}
		}
		return score;

	}

	//might want it to return a double
	private int[] depth = new int[1];
	private int monteCarlo(Role role, MachineState state, int count, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int total = 0;
		for (int i=0; i< count; i++) {
			if (timeout - System.currentTimeMillis() <= TIMEOUT_MARGIN2)
   		        break;
			MachineState finalState = getStateMachine().performDepthCharge(state, depth);
			total += getStateMachine().getGoal(finalState, role);
		}
		return total/count;

	}

}