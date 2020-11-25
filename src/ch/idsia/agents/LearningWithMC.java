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
import ch.idsia.agents.MCAgent;
import ch.idsia.agents.MCStateActionPair;

public class LearningWithMC implements LearningAgent {
	private final String FILENAME = "MonteCarlo.txt";

	private MCAgent agent;
	private String name = "LearningWithMC";
	// 目標値(4096.0はステージの右端)
	private float goal = 4096.0f;
	private String args;
	// 試行回数
	private int numOfTrial = 50000;

	// コンストラクタ
	public LearningWithMC(String args) {
		this.args = args;
		agent = new MCAgent();
		MCAgent.bestScore = 0;

		try {
			File f = new File(FILENAME);
			BufferedReader br = new BufferedReader(new FileReader(f));
			for (int i = 0; i < MCState.N_STATES; ++i) {
				for (int j = 0; j < MCAgent.N_ACTIONS; ++j) {
					String s = br.readLine();
					MCAgent.sumValue[i][j] = Float.parseFloat(s);
					s = br.readLine();
					MCAgent.num[i][j] = Integer.parseInt(s);
					MCAgent.qValue[i][j] = MCAgent.sumValue[i][j] / MCAgent.num[i][j];
				}
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// 学習部分
	// 学習してその中でもっとも良かったものをリプレイ
	public void learn() {
		final int SHOW_INTERVALS = 1000;

		long startTime = System.currentTimeMillis();
		for (int nt = 0; nt < numOfTrial; ++nt) {
			// 目標値までマリオが到達したらshowして終了
			if (run() >= 4096.0f) {
				show();
				break;
			}
			if (nt % SHOW_INTERVALS == 999) {

				long endTime = System.currentTimeMillis();
				System.out.println("time for " + SHOW_INTERVALS + " plays:" + (endTime - startTime) + "(ms)");

				show();

				try {
					// 学習した行動価値関数を書き込み
					File f = new File(FILENAME);
					f.createNewFile();
					BufferedWriter bw = new BufferedWriter(new FileWriter(f));
					for (int i = 0; i < MCState.N_STATES; ++i) {
						for (int j = 0; j < MCAgent.N_ACTIONS; ++j) {
							bw.write(String.valueOf(MCAgent.bestSumValue[i][j]));
							bw.newLine();
							bw.write(String.valueOf(MCAgent.bestNum[i][j]));
							bw.newLine();
						}
					}
					bw.close();
				} catch (IOException e) {
					System.out.println(e);
				}

				startTime = System.currentTimeMillis();
			}

		}

	}

	// リプレイ
	public void show() {
		MCAgent.ini();
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		MCAgent.setMode(true);

		/* プレイ画面出力するか否か */
		marioAIOptions.setVisualization(true);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		// 報酬取得
		float reward = evaluationInfo.distancePassedPhys;
		System.out.println("報酬は" + reward);
	}

	// 学習
	// 画面に表示はしない
	public float run() {
		long startTime = System.currentTimeMillis();

		MCAgent.ini();
		/* MCAgentをプレイさせる */
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		MCAgent.setMode(false);

		/* プレイ画面出力するか否か */
		marioAIOptions.setVisualization(false);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		// 報酬取得
		float reward = evaluationInfo.distancePassedPhys;

		/* Normalize num values when they are too large and slowing down learning */
		if (MCAgent.getMaxNum() > 10000) {
			for (int i = 0; i < MCState.N_STATES; ++i) {
				for (int j = 0; j < MCAgent.N_ACTIONS; ++j) {
					if (MCAgent.num[i][j] > 3) {
						MCAgent.num[i][j] /= 2;
						MCAgent.sumValue[i][j] /= 2;
					}
				}
			}
		}

		// ベストスコアが出たら更新
		if (reward > MCAgent.bestScore) {
			MCAgent.bestScore = reward;
			MCAgent.best = new ArrayList<Integer>(MCAgent.actions);
			MCAgent.bestSumValue = new float[MCState.N_STATES][MCAgent.N_ACTIONS];
			for (int i = 0; i < MCState.N_STATES; ++i) {
				MCAgent.bestSumValue[i] = MCAgent.sumValue[i].clone();
			}
			MCAgent.bestNum = new int[MCState.N_STATES][MCAgent.N_ACTIONS];
			for (int i = 0; i < MCState.N_STATES; ++i) {
				MCAgent.bestNum[i] = MCAgent.num[i].clone();
			}
		}
		Iterator<MCStateActionPair> itr = MCAgent.selected.keySet().iterator();
		// Iterator<MCStateActionPair> itr = MCAgent.history.iterator();
		// 価値関数を更新
		while (itr.hasNext()) {
			MCStateActionPair key = (MCStateActionPair) itr.next();
			// MCStateActionPair key = (MCStateActionPair) itr.next();
			int s = key.getState().toInt();
			int a = key.getAction();
			MCAgent.sumValue[s][a] += reward;
			MCAgent.num[s][a]++;
			MCAgent.qValue[s][a] = MCAgent.sumValue[s][a] / (float) MCAgent.num[s][a];
		}

		long endTime = System.currentTimeMillis();
		System.out.println(reward + " s:" + MCAgent.getNumberOfVisitedSAs() + " M(num):" + MCAgent.getMaxNum() + " t:"
				+ (endTime - startTime) + "(ms)");

		return reward;
	}

	////////////////////////////// ここからは必要なし//////////////////////////////
	@Override
	public boolean[] getAction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void integrateObservation(Environment environment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void giveIntermediateReward(float intermediateReward) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void giveReward(float reward) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newEpisode() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLearningTask(LearningTask learningTask) {
		// TODO Auto-generated method stub

	}

	@Override
	public Agent getBestAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEvaluationQuota(long num) {
		// TODO Auto-generated method stub

	}
}