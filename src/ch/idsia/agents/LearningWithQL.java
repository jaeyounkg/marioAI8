package ch.idsia.agents;

import java.io.*;
import java.util.*;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;
import ch.idsia.utils.wox.serial.Easy;
import ch.idsia.agents.QLAgent;
import ch.idsia.agents.QLStateAction;

public class LearningWithQL implements LearningAgent {
	private final String FILENAME = "LearnedQLModel.txt";

	private QLAgent agent;
	private String name = "LearningWithQL";
	// 目標値(4096.0はステージの右端)
	private float goal = 4096.0f;
	private String args;
	// 試行回数
	private int numOfTrial = 100000000;

	private boolean runExploratively = true;

	private double bestScore = 1;
	private List<QLStateAction> bestHistory;
	// explorative
	private double curEBestScore = 1;
	private List<Integer> curEBestActions;
	// non-explorative
	private double bestNScore = 1;
	private QLQFunction bestNQ;
	private List<QLStateAction> bestNHistory;

	private double prevNBestScore = 1;
	private double curNBestScore = 1;
	private QLQFunction curNBestQ;
	private List<Integer> curNBestActions;
	private Random random = new Random();

	// コンストラクタ
	public LearningWithQL(String args) {
		this.args = args;
		agent = new QLAgent();

		try {
			agent.loadModelFromFile(FILENAME);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static double calculateReward(double score) {
		return Math.pow(Math.max(score, 0) / 2048, 2) * 2048;
	}

	// 学習部分
	// 学習してその中でもっとも良かったものをリプレイ
	public void learn() {
		final int SHOW_INTERVALS = 500;

		long startTime = System.currentTimeMillis();
		for (int nt = 0; nt < numOfTrial; ++nt) {
			// 目標値までマリオが到達したらshowして終了
			if (run() >= 4096.0f && !runExploratively) {
				show(agent.actions);

				try {
					QLAgent.saveModelToFile(bestNQ, FILENAME);
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}

				break;
			}
			if (nt % SHOW_INTERVALS == SHOW_INTERVALS - 1) {

				long endTime = System.currentTimeMillis();

				System.out.println("time for " + SHOW_INTERVALS + " plays:" + (endTime - startTime) + "(ms)");
				System.out.println("current (e) best score: " + curEBestScore);
				System.out.println("current (n) best score: " + curNBestScore);
				System.out.println("all time (n) best score: " + bestNScore);
				System.out.println("all time best score: " + bestScore);

				try {
					QLAgent.saveModelToFile(bestNQ, FILENAME);
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}

				// if (curNBestScore > prevNBestScore)
				show(curNBestActions);
				prevNBestScore = bestNScore;

				curEBestScore = 0;
				curNBestScore = 0;

				// agent.history = bestNHistory;
				// for (int i = 0; i < SHOW_INTERVALS / 5; ++i)
				// agent.giveRewardForEntireHistory(calculateReward(bestNScore));

				startTime = System.currentTimeMillis();
			}

		}

	}

	// リプレイ
	public void show(List<Integer> actions) {
		agent.ini(false, true);
		agent.actions = new ArrayList<Integer>(actions);
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		marioAIOptions.setArgs(this.args);
		marioAIOptions.setVisualization(true);
		marioAIOptions.setAgent(agent);
		marioAIOptions.setFPS(100);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		float reward = evaluationInfo.distancePassedPhys;
		System.out.println("報酬は" + reward);
	}

	// 学習
	// 画面に表示はしない
	public double run() {
		long startTime = System.currentTimeMillis();

		runExploratively = !runExploratively;
		if (runExploratively)
			agent.epsilon = random.nextFloat() % 0.2f;
		else
			agent.epsilon = 0f;

		agent.ini(runExploratively, false);
		agent.setExplorativeMaxDist((float) bestNScore);

		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);
		marioAIOptions.setArgs(this.args);
		marioAIOptions.setVisualization(false);
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		// 報酬取得
		double score = evaluationInfo.distancePassedPhys;

		// ベストスコアが出たら更新
		if (score > bestScore) {
			bestScore = score;
			bestHistory = new ArrayList<QLStateAction>();
			for (QLStateAction sa : agent.history) {
				bestHistory.add(sa.clone());
			}
		}
		if (runExploratively && score > curEBestScore) {
			curEBestScore = score;
			curEBestActions = new ArrayList<Integer>(agent.actions);
		}
		if (!runExploratively && score > curNBestScore) {
			curNBestScore = score;
			curNBestActions = new ArrayList<Integer>(agent.actions);
			curNBestQ = agent.Q.clone();
		}
		if (!runExploratively && score > bestNScore) {
			bestNScore = score;
			bestNQ = agent.Q.clone();
			bestNHistory = new ArrayList<QLStateAction>();
			for (QLStateAction sa : agent.history) {
				bestNHistory.add(sa.clone());
			}
		}

		// double reward = Math.pow(Math.max(score, 0) / Math.max(500,
		// bestScore), 2.5) * 1000;
		double reward = calculateReward(score);
		if (evaluationInfo.timeSpent >= 199) {
			ListIterator<QLStateAction> iter = agent.history.listIterator(0);
			int t = 0;
			for (; iter.hasNext(); ++t) {
				QLStateAction sa = iter.next();
				if (sa.state.marioX >= score - 0.1f)
					break;
			}
			agent.giveRewardForPastActions(0, t, reward);
			agent.giveRewardForPastActions(t + 1, agent.history.size(), 0);
		} else {
			agent.giveRewardForEntireHistory(reward);
		}
		double reward2 = score / evaluationInfo.timeSpent * 10;
		// agent.giveRewardForEntireHistory(reward2);

		long endTime = System.currentTimeMillis();
		System.out.println((runExploratively ? "(e) " : "(n) ") + score + " r:" + (int) reward + "," + (int) reward2
				+ " s:" + agent.Q.size() + " g:" + evaluationInfo.timeSpent + "(s) t:" + (endTime - startTime)
				+ "(ms) e:" + agent.epsilon);

		agent.history = new ArrayList<QLStateAction>();
		for (QLStateAction sa : bestHistory) {
			agent.history.add(sa.clone());
		}
		agent.giveRewardForPastActions(0, 15 * 0, calculateReward(bestScore));
		// agent.giveRewardForEntireHistory(calculateReward(bestScore));

		return score;
	}

	////////////////////////////// ここからは必要なし//////////////////////////////
	public boolean[] getAction() {
		// TODO Auto-generated method stub
		return null;
	}

	public void integrateObservation(Environment environment) {
		// TODO Auto-generated method stub

	}

	public void giveIntermediateReward(float intermediateReward) {
		// TODO Auto-generated method stub

	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	public void giveReward(float reward) {
		// TODO Auto-generated method stub

	}

	public void newEpisode() {
		// TODO Auto-generated method stub

	}

	public void setLearningTask(LearningTask learningTask) {
		// TODO Auto-generated method stub

	}

	public Agent getBestAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	public void init() {
		// TODO Auto-generated method stub

	}

	public void setEvaluationQuota(long num) {
		// TODO Auto-generated method stub

	}
}