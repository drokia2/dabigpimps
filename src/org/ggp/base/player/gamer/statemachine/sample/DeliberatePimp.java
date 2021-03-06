package org.ggp.base.player.gamer.statemachine.sample;

import java.util.Arrays;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.gdl.grammar.GdlPool;
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
public final class DeliberatePimp extends SampleGamer
{
	//Milliseconds before timeout where we must bail
	public final static int timeoutMargin = 50;
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

	private List<Move> makeMovesArray(Move move) {
		Move[] moves = new Move[getStateMachine().getRoles().size()];
		for (int j=0; j<moves.length; j++) {
			moves[j] =  new Move(GdlPool.getConstant("noop"));
		}
		int index = getStateMachine().getRoleIndices().get(getRole());
		moves[index] = move;
		return Arrays.asList(moves);
	}

	private Move bestMove(Role role, MachineState state, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		Move bestAction = legalMoves.get(0);
		if (legalMoves.size() == 1) return bestAction;
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);
			//make moves array

			int result = maxScore(role, getStateMachine().getNextState(state, makeMovesArray(move)), timeout);

			if (result == 100)  {
				return move;
			}
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

	private int  maxScore(Role role, MachineState state, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state))  {
			return getStateMachine().getGoal(state, role);
		}
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);

			int result = maxScore(role, getStateMachine().getNextState(state, makeMovesArray(move)), timeout);
			if (result > score) {
				score = result;
			}
			if (timeout - System.currentTimeMillis() <= timeoutMargin){
				break;
			}
		}
		return score;

	}
}