package io.cockroachdb.pool.configurer.web;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.Valid;

import io.cockroachdb.pool.configurer.metrics.Metrics;
import io.cockroachdb.pool.configurer.model.ConfigModel;
import io.cockroachdb.pool.configurer.model.DataSourceConfigPinnedEvent;
import io.cockroachdb.pool.configurer.web.support.MessagePublisher;
import io.cockroachdb.pool.configurer.web.support.TopicName;
import io.cockroachdb.pool.configurer.workload.WorkloadForm;
import io.cockroachdb.pool.configurer.workload.WorkloadManager;
import io.cockroachdb.pool.configurer.workload.WorkloadModel;
import io.cockroachdb.pool.configurer.workload.WorkloadType;
import io.cockroachdb.pool.configurer.workload.WorkloadUpdatedEvent;

@WebController
@RequestMapping("/workload")
@SessionAttributes("configModel")
public class WorkloadController {
    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private MessagePublisher messagePublisher;

    private final Map<String, ConfigModel> poolConfigModels = new HashMap<>();

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void modelUpdate() {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_UPDATE, null);
    }

    @EventListener
    public void handle(DataSourceConfigPinnedEvent event) {
        ConfigModel configModel = event.getConfigModel();
        Objects.requireNonNull(configModel.getPoolName(), "pool name is required");
        poolConfigModels.put(configModel.getPoolName().toLowerCase(), configModel);
    }

    @EventListener
    public void handle(WorkloadUpdatedEvent event) {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_REFRESH, null);
    }

    @ModelAttribute("workloadForm")
    public WorkloadForm workloadFormModel(
            @ModelAttribute(value = "configModel", binding = false) ConfigModel configModel
    ) {
        WorkloadForm form = new WorkloadForm();
        form.setCount(10);
        form.setProbability(0.75);
        form.setWaitTime(0);
        form.setWaitTimeVariation(0);
        form.setDuration(Duration.ofMinutes(15).toSeconds());
        form.setWorkloadType(WorkloadType.select_one);
        form.setSlots(configModel.getOccupiedSlots());
        return form;
    }

    @GetMapping
    public Callable<String> indexPage(@PageableDefault(size = 15) Pageable page,
                                      Model model) {

        Page<WorkloadModel> workloadPage = workloadManager.getWorkloads(page,
                workload -> {
                    return true;
                });

        model.addAttribute("workloadPage", workloadPage);
        model.addAttribute("aggregatedMetrics", workloadManager.getMetricsAggregate(page));

        return () -> "workload";
    }

    @PostMapping(params = "action=add")
    public Callable<String> submitWorkloadForm(@ModelAttribute("workloadForm")
                                               @Valid WorkloadForm workloadForm,
                                               BindingResult bindingResult,
                                               Model model) {
        if (Objects.isNull(workloadForm.getSlot())) {
            bindingResult.addError(new ObjectError("globalError",
                    "No datasource slot selected!"));
        } else {
            final ConfigModel configModel;
            if (Objects.nonNull(workloadForm.getSlot())) {
                configModel = poolConfigModels.get(workloadForm.getSlot().getDisplayName().toLowerCase());
            } else {
                configModel = null;
            }

            if (Objects.isNull(configModel)) {
                bindingResult.addError(new ObjectError("globalError",
                        "No pool config for selected slot"));
            }

            workloadManager.submitWorkloads(workloadForm, configModel);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            model.addAttribute("workloadForm", workloadForm);
            model.addAttribute("workloadPage", Page.empty());
            return () -> "workload";
        }

        return () -> "redirect:workload";
    }

    @GetMapping("{id}")
    public Callable<String> getWorkloadDetails(@PathVariable("id") Integer id, Model model) {
        return () -> {
            model.addAttribute("workload", workloadManager.getWorkloadById(id));
            return "workload-detail";
        };
    }

    @PostMapping(value = "/cancelAll")
    public RedirectView cancelAllWorkloads() {
        workloadManager.cancelWorkloads();
        return new RedirectView("/workload");
    }

    @PostMapping(value = "/deleteAll")
    public RedirectView deleteAllWorkloads() {
        workloadManager.deleteWorkloads();
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/cancel/{id}")
    public RedirectView cancelWorkload(@PathVariable("id") Integer id) {
        workloadManager.cancelWorkload(id);
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/delete/{id}")
    public RedirectView deleteWorkload(@PathVariable("id") Integer id) {
        workloadManager.deleteWorkload(id);
        return new RedirectView("/workload");
    }

    @GetMapping("/items")
    public @ResponseBody List<WorkloadModel> getWorkloadItems(Pageable page) {
        return workloadManager.getWorkloads(page, (x) -> true).getContent();
    }

    @GetMapping("/summary")
    public @ResponseBody Metrics getWorkloadSummary(Pageable page) {
        return workloadManager.getMetricsAggregate(page);
    }
}
