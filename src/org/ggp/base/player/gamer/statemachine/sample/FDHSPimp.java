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
public final class FDHSPimp extends SampleGamer
{
	//Milliseconds before timeout where we must bail
		public final static int timeoutMargin = 50;
		public final static int LEVEL_LIMIT = 9;

	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.
	 *
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();


		/**
		 * We put in memory the list of legal moves from the
		 * current state. The goal of every stateMachineSelectMove()
		 * is to return one of these moves. The choice of which
		 * Move to play is the goal of GGP.
		 */
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		// SampleLegalGamer is very simple : it picks the last legal move
		Move selection = bestMove(getRole(), getCurrentState(), timeout);

		System.out.println("Best move was: " + selection.toString());
		// We get the end time
		// It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stopzb  and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

//	private List<Move> makeMovesArray(Move move) {
//		Move[] moves = new Move[getStateMachine().getRoles().size()];
//		for (int j=0; j<moves.length; j++) {
//			moves[j] =  new Move(GdlPool.getConstant("noop"));
//		}
//		int index = getStateMachine().getRoleIndices().get(getRole());
//		moves[index] = move;
//		return Arrays.asList(moves);
//	}

	private Move bestMove(Role role, MachineState state, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		Move bestAction = legalMoves.get(0);
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);
			//System.out.println("ACTION:" + move.toString());
			int result = minScore(role, move, state, 0, 100, timeout, 0);
			if (result == 100) return move;
			if (result > score) {
				score = result;
				bestAction = move;
			}
			if (timeout - System.currentTimeMillis() <= timeoutMargin){
				break;
			}
		}
		return bestAction;
	}

	private int  minScore(Role role, Move move, MachineState state, int alpha, int beta, long timeout, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		//Role opponent = getStateMachine().getRoles().get(0) == getRole() ? getStateMachine().getRoles().get(1) : getStateMachine().getRoles().get(0);
		//if (getStateMachine().isTerminal(state))  {
		//	return getStateMachine().getGoal(state, role);
		//}
		//List<Move> legalMoves = getStateMachine().getLegalMoves(state, opponent);
		List<List<Move>> legalJointMoves = getStateMachine().getLegalJointMoves(state, role, move);
		//int score = 100;
		for (int i = 0; i < legalJointMoves.size(); i++) {
			List<Move> currMoves = legalJointMoves.get(i);
			MachineState nextState = getStateMachine().getNextState(state, currMoves);
			int result = maxScore(role, nextState, alpha, beta, timeout, level + 1);
			//if (result < score) {
			//	score = result;
			//}
			beta = Math.min(beta,  result);
			if (beta <= alpha) return alpha;
			if (timeout - System.currentTimeMillis() <= timeoutMargin){
				break;
			}
		}
		return beta;

	}

	private int  maxScore(Role role, MachineState state, int alpha, int beta, long timeout, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state))  {
			return getStateMachine().getGoal(state, role);
		}
		if (level >= LEVEL_LIMIT) return heuristicFn(role, state);
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		//int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			Move currMove = legalMoves.get(i);

			int result = minScore(role, currMove, state, alpha, beta, timeout, level);
			//if (result > score) {
			//	score = result;
			//}
			alpha = Math.max(alpha,  result);
			if (alpha >= beta) return beta;
			if (timeout - System.currentTimeMillis() <= timeoutMargin){
				break;
			}
		}
		return alpha;

	}

	private int heuristicFn(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		// TODO Auto-generated method stub
		int heuristicNum = 1;
		int numFeasible = 4;
		if (heuristicNum == 1) //goal proximity
			return getStateMachine().getGoal(state, role);
		else if (heuristicNum == 2){ //mobility
			int numLegalMoves = getStateMachine().getLegalMoves(state, role).size();
			return (int) (numLegalMoves < numFeasible ? (double)numLegalMoves/numFeasible * 100 : 100);
		} else { //focus
			int numLegalMoves = getStateMachine().getLegalMoves(state, role).size();
			return 100 - (int) (numLegalMoves < numFeasible ? (double)numLegalMoves/numFeasible * 100 : 100);
		}
	}

	//Exercise 7.4.1
	//numFeasible = 4, heuristic = mobility, LEVEL_LIMIT = 9
}