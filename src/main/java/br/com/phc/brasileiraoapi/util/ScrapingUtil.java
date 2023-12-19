package br.com.phc.brasileiraoapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.phc.brasileiraoapi.dto.PartidaGoogleDTO;

public class ScrapingUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapingUtil.class);

	private static final String BASE_URL_GOOGLE = "https://www.google.com/search?q=";
	
	private static final String COMPLEMENTO_URL_GOOGLE = "&hl=pt-BR";
	
	private static final String CASA = "casa";
	
	private static final String VISITANTE = "visitante";
	
	public static void main(String[] args) {

		String url = BASE_URL_GOOGLE + "internacional+x+gremio" + COMPLEMENTO_URL_GOOGLE;
		
		ScrapingUtil scraping = new ScrapingUtil();
		
		scraping.obterInformacoesPartida(url);
		
	}

	public PartidaGoogleDTO obterInformacoesPartida(String url) {
		PartidaGoogleDTO partida = new PartidaGoogleDTO();
		
		Document document = null;
		
		try {
			document = Jsoup.connect(url).get();
			
			String title = document.title();
			LOGGER.info("Titulo da pagina: {}", title);
			
			StatusPartida statusPartida = obtemStatusPartida(document);
			LOGGER.info("Status Partida: {}", statusPartida);
			
			if (statusPartida != StatusPartida.PARTIDA_NAO_INICIADA) {
				String tempoPartida = obtemTempoPartida(document);
				LOGGER.info("Tempo partida: {}", tempoPartida);
				
				Integer placarEquipeCasa = recuperarPlacarEquipeCasa(document);
				LOGGER.info("Placar equipe casa: {}", placarEquipeCasa);
				
				Integer placarEquipeVisitante = recuperarPlacarEquipeVisitante(document);
				LOGGER.info("Placar equipe visitante: {}", placarEquipeVisitante);
				
				String golsEquipeCasa = recuperaGolsEquipeCasa(document);
				LOGGER.info("Gols equipe casa: {}", golsEquipeCasa);
				
				String golsEquipeVisitaante = recuperaGolsEquipeVisitante(document);
				LOGGER.info("Gols equipe visitante: {}", golsEquipeVisitaante);
				
				Integer placaaEstendidoEquipeCasa = buscarPenalidaes(document, CASA);
				LOGGER.info("placar estendido equipe casa: {}", placaaEstendidoEquipeCasa);
				
				Integer placaaEstendidoEquipeVisitante = buscarPenalidaes(document, VISITANTE);
				LOGGER.info("placar estendido equipe visitante: {}", placaaEstendidoEquipeVisitante);
			}
			
			
			String nomeEquipeCasa = recuperaNomeEquipeCasa(document);
			LOGGER.info("Nome Equipe Casa: {}", nomeEquipeCasa);
			
			String nomeEquipeVisitante = recuperaNomeEquipeVisitante(document);
			LOGGER.info("Nome Equipe Visitante: {}", nomeEquipeVisitante);
			
			String urlLogoEquipeCasa = recuperaLogoEquipeCasa(document);
			LOGGER.info("Url logo equipe casa: {}", urlLogoEquipeCasa);
			
			String urlLogoEquipeVisitante = recuperaLogoEquipeVisitante(document);
			LOGGER.info("Url logo equipe visitante: {}", urlLogoEquipeVisitante);
			
			
			
		} catch (IOException e) {
			LOGGER.error("ERRO AO TENTARA CONECTAR NO GOOGLE COM JSOUP -> {}", e.getMessage());
		}
		
		return partida;
	}
	
	public StatusPartida obtemStatusPartida(Document document) {
		//Situações
		//1 - paratida nao iniciada
		//2 - partida inicia
		//3 - partida encerrada
		//4 - penalidades
		StatusPartida statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;
		
		boolean isTempoPaartida = document.select("div[class=imso_mh__lv-m-stts-cont]").isEmpty();
		if (!isTempoPaartida) {
			String tempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").first().text();
			statusPartida = StatusPartida.PARTIDA_EM_ANDAMENTO;
			if (tempoPartida.contains("Pênaltis")) {
				statusPartida = StatusPartida.PARTIDA_PENALTIS;
			}
			LOGGER.info(tempoPartida);
		}
		isTempoPaartida = document.select("spaan[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]").isEmpty();
		
		if (!isTempoPaartida) {
			statusPartida = StatusPartida.PARTIDA_ENCERRADA;
		}
		LOGGER.info(statusPartida.toString());
		return statusPartida;
	}
	
	public String obtemTempoPartida(Document document) {
		String tempoPartida = null;
		//Jogo rolando ou intervaalo ou penalaidades
		boolean isTempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").isEmpty();
		if (!isTempoPartida) {
			tempoPartida = document.select("div[class=imso_mh__lv-m-stts-cont]").first().text();
		}
		
		isTempoPartida = document.select("span[class=imso_mh__ft-mtch imso_medium-font imso_mh__ft-mtchc]").isEmpty();
		if (!isTempoPartida) {
			tempoPartida = document.select("span[class=imso_mh__ft-mtch imso_medium-font imso_mh__ft-mtchc]").first().text();
		}
		
		LOGGER.info(corrigeTempoPartida(tempoPartida));
		return corrigeTempoPartida(tempoPartida);
	}
	
	public String corrigeTempoPartida(String tempo) {
		if (tempo.contains("'")) {
			return tempo.replace(" ", "").replace("'", " min");
		} else {
			return tempo;
		}
	}
	
	public String recuperaNomeEquipeCasa(Document document) {
		String nomeEquipe = null;
		Element element = document.selectFirst("div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]");
		if (element != null) {
			nomeEquipe = element.select("span").text();
		}
		
		return nomeEquipe;
	}
	
	public String recuperaNomeEquipeVisitante(Document document) {
		String nomeEquipe = null;
		Element element = document.selectFirst("div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]");
		if (element != null) {
			nomeEquipe = element.select("span").text();
		}
		
		return nomeEquipe;
	}
	
	public String recuperaLogoEquipeCasa(Document document) {
		String urlLogo = null;
		Element elemento = document.selectFirst("div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]");
		if (elemento != null) {
			urlLogo = "https:" + elemento.select("img[class=imso_btl__mh-logo]").attr("src");
		}
		
		return urlLogo;
	}
	
	public String recuperaLogoEquipeVisitante(Document document) {
		String urlLogo = null;
		Element elemento = document.selectFirst("div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]");
		if (elemento != null) {
			urlLogo = "https:" + elemento.select("img[class=imso_btl__mh-logo]").attr("src");
		}
		
		return urlLogo;
	}
	
	public Integer recuperarPlacarEquipeCasa(Document document) {
		String placarEquipe = document.selectFirst("div[class=imso_mh__l-tm-sc imso_mh__scr-it imso-light-font]").text();
		
		return formataPlacarStringInteger(placarEquipe);
	}
	
	public Integer recuperarPlacarEquipeVisitante(Document document) {
		String placarEquipe = document.selectFirst("div[class=imso_mh__r-tm-sc imso_mh__scr-it imso-light-font]").text();
		
		return formataPlacarStringInteger(placarEquipe);
	}
	
	public String recuperaGolsEquipeCasa(Document document) {
		List<String> golsEquipe = new ArrayList<>();
		
		Elements elementos = document.select("div[class=imso_gs__tgs imso_gs__left-team]").select("div[class=imso_gs__gs-r]");
		
		for (Element e : elementos) {
			String infoGol = e.select("div[class=imso_gs__gs-r]").text();
			golsEquipe.add(infoGol);
		}
		
		
		return String.join(", ", golsEquipe);
	}
	
	public String recuperaGolsEquipeVisitante(Document document) {
		List<String> golsEquipe = new ArrayList<>();
		
		Elements elementos = document.select("div[class=imso_gs__tgs imso_gs__right-team]").select("div[class=imso_gs__gs-r]");
		
		for (Element e : elementos) {
			String infoGol = e.select("div[class=imso_gs__gs-r]").text();
			golsEquipe.add(infoGol);
		}
		
		
		return String.join(", ", golsEquipe);
	}
	
	public Integer buscarPenalidaes(Document document, String tipoEquipe) {
		boolean isPenalidade = document.select("div[class=imso_mh_s__psn-sc]").isEmpty();
		if (!isPenalidade) {
			String penalidaades = document.select("div[class=imso_mh_s__psn-sc]").text();
			String penalidadesCompleta = penalidaades.substring(0, 5).replace(" ", "");
			String[] divisao = penalidadesCompleta.split("-");
			return tipoEquipe.equals(CASA) ? formataPlacarStringInteger(divisao[0]) : formataPlacarStringInteger(divisao[1]);
		}
		return null;
	}
	
	public Integer formataPlacarStringInteger(String placar) {
		Integer valor;
		try {
			valor = Integer.parseInt(placar);
		} catch (Exception e) {
			valor = 0;
		}
		
		return valor;
	}
}
